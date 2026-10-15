import re, io, json, time, zipfile, urllib.request
import numpy as np, pandas as pd
from scipy.sparse import vstack
from sklearn.base import clone
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split, StratifiedKFold, cross_val_score, cross_val_predict
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.naive_bayes import MultinomialNB
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import LinearSVC
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, matthews_corrcoef, confusion_matrix

URL = "https://archive.ics.uci.edu/static/public/228/sms+spam+collection.zip"
z = zipfile.ZipFile(io.BytesIO(urllib.request.urlopen(URL).read()))
df = pd.read_csv(z.open("SMSSpamCollection"), sep="\t", header=None, names=["label", "text"])
df["y"] = (df.label == "spam").astype(int)
df = df.drop_duplicates(subset="text").reset_index(drop=True)

TOKEN = r"[a-z0-9_]{2,}|[£$€]"
def clean(t):
    t = t.lower()
    t = re.sub(r"https?://\S+|www\.\S+", " urltoken ", t)
    t = re.sub(r"\d{5,}", " longnum ", t)
    t = re.sub(r"[^a-z0-9\s£$€_]", " ", t)
    return re.sub(r"\s+", " ", t).strip()

idx_tr, idx_te = train_test_split(df.index, test_size=0.2, stratify=df.y, random_state=42)
idx_core, idx_val = train_test_split(idx_tr, test_size=0.1, stratify=df.y[idx_tr], random_state=7)
T = lambda ix: df.loc[ix, "text"].map(clean)
Xc, Xv, Xt = T(idx_core), T(idx_val), T(idx_te)
yc, yv, yt = df.y[idx_core].values, df.y[idx_val].values, df.y[idx_te].values

def vec(**kw):
    p = dict(lowercase=False, token_pattern=TOKEN, ngram_range=(1, 2), max_features=5000, sublinear_tf=True, norm="l2")
    p.update(kw)
    return TfidfVectorizer(**p)

V = vec().fit(Xc)
Ac, At = V.transform(Xc), V.transform(Xt)

def metrics(y, p):
    tn, fp, fn, tp = confusion_matrix(y, p).ravel()
    return dict(acc=accuracy_score(y, p), prec=precision_score(y, p), rec=recall_score(y, p),
                f1=f1_score(y, p), mcc=matthews_corrcoef(y, p), cm=[[int(tn), int(fp)], [int(fn), int(tp)]])

SVC_KW = dict(C=1.0, class_weight="balanced", max_iter=1000)
models = {
    "Naive Bayes": MultinomialNB(alpha=1.0),
    "Logistic Regression": LogisticRegression(C=10, class_weight="balanced", max_iter=1000),
    "Random Forest": RandomForestClassifier(n_estimators=300, random_state=42, n_jobs=-1),
    "Linear SVM": LinearSVC(**SVC_KW),
}
bench = {}
skf = StratifiedKFold(5, shuffle=True, random_state=42)
for name, m in models.items():
    t0 = time.perf_counter(); m.fit(Ac, yc); tr = time.perf_counter() - t0
    t0 = time.perf_counter(); pred = m.predict(At); inf = (time.perf_counter() - t0) / At.shape[0] * 1000
    cv = cross_val_score(Pipeline([("v", vec()), ("m", clone(m))]), Xc, yc, cv=skf, scoring="f1")
    bench[name] = dict(metrics(yt, pred), train_s=tr, infer_ms=inf, cv_f1_mean=cv.mean(), cv_f1_std=cv.std())

# Deployed model: Linear SVM + Platt calibration
svc = LinearSVC(**SVC_KW).fit(Ac, yc)
s_cv = cross_val_predict(LinearSVC(**SVC_KW), Ac, yc, cv=5, method="decision_function")
pl = LogisticRegression(C=1e6).fit(s_cv.reshape(-1, 1), yc)
A, B = float(pl.coef_[0][0]), float(pl.intercept_[0])
sig = lambda z: 1 / (1 + np.exp(-z))
pt = sig(A * svc.decision_function(At) + B)

# Threshold sensitivity (symmetric: block if p>=tau, allow if p<=1-tau)
thr = []
for tau in [0.60, 0.70, 0.80, 0.90, 0.95]:
    blk, alw = pt >= tau, pt <= 1 - tau
    rev = ~(blk | alw); auto = ~rev
    thr.append(dict(tau=tau, review_pct=float(rev.mean() * 100), auto_fp=int((blk & (yt == 0)).sum()),
                    auto_fn=int((alw & (yt == 1)).sum()), n_review=int(rev.sum()), n=int(len(yt)),
                    auto_acc=float((((blk & (yt == 1)) | (alw & (yt == 0))).sum() / max(auto.sum(), 1)) * 100)))
# fix auto_acc: correct auto decisions / auto decisions
for r, tau in zip(thr, [0.60, 0.70, 0.80, 0.90, 0.95]):
    blk, alw = pt >= tau, pt <= 1 - tau
    ok = ((blk & (yt == 1)) | (alw & (yt == 0))).sum()
    r["auto_acc"] = float(ok / max((blk | alw).sum(), 1) * 100)

# Feedback simulation
perm = np.random.RandomState(42).permutation(len(yt)); half = len(yt) // 2
stream, hold = perm[:half], perm[half:]
esc = stream[(pt[stream] > 0.2) & (pt[stream] < 0.8)]
curve = []
for k in [0, 25, 50, 100, len(esc)]:
    k = min(k, len(esc)); ex = esc[:k]
    m = LinearSVC(**SVC_KW).fit(vstack([Ac, At[ex]]), np.concatenate([yc, yt[ex]]))
    pr = m.predict(At[hold])
    curve.append(dict(k=int(k), prec=precision_score(yt[hold], pr), rec=recall_score(yt[hold], pr), f1=f1_score(yt[hold], pr)))

# Ablation
def ablate(vkw=None, ckw=None):
    v = vec(**(vkw or {})).fit(Xc)
    m = LinearSVC(**{**SVC_KW, **(ckw or {})}).fit(v.transform(Xc), yc)
    return metrics(yt, m.predict(v.transform(Xt)))
abl = {"Full model": ablate(), "Unigrams only": ablate(dict(ngram_range=(1, 1))),
       "No class weights": ablate(None, dict(class_weight=None)), "Stop-words removed": ablate(dict(stop_words="english")),
       "Raw term frequency": ablate(dict(sublinear_tf=False, use_idf=False)), "Vocabulary 1,000": ablate(dict(max_features=1000))}

# Global top terms
w = svc.coef_[0]; inv = {i: t for t, i in V.vocabulary_.items()}
order = np.argsort(w)
top_ham = [(inv[i], float(w[i])) for i in order[:20]]
top_spam = [(inv[i], float(w[i])) for i in order[::-1][:20]]

import os; os.makedirs("assets", exist_ok=True)
J = lambda o, f: json.dump(o, open(f"assets/{f}", "w"), default=lambda x: float(x), ensure_ascii=False)
J(dict(vocab={t: int(i) for t, i in V.vocabulary_.items()}, idf=V.idf_.tolist(), weights=w.tolist(),
       bias=float(svc.intercept_[0]), plattA=A, plattB=B, version=1), "model.json")
J(dict(models=bench, threshold=thr, feedback=curve, ablation=abl, top_spam=top_spam, top_ham=top_ham,
       n_total=int(len(df)), n_spam=int(df.y.sum())), "benchmark.json")
val = df.loc[idx_val]; J([dict(y=int(r.y), text=r.text) for r in val.itertuples()], "validation.json")
# Parity set: Kotlin must reproduce these probabilities within 1e-3
par = df.loc[idx_te].sample(60, random_state=1)
pp = sig(A * svc.decision_function(V.transform(par.text.map(clean))) + B)
J([dict(text=t, p=float(p)) for t, p in zip(par.text, pp)], "parity.json")
print("Done. Copy the 4 JSON files from ./assets to app/src/main/assets/")
