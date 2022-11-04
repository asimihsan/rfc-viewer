from sklearn.feature_extraction.text import TfidfVectorizer
import numpy as np
from scipy import sparse
from pprint import pprint

from rfc_wrapper import RFCPreprocessedWrapper, Rfc
import pathlib


# https://gist.github.com/koreyou/f3a8a0470d32aa56b32f198f49a9f2b8
class BM25(object):
    def __init__(self, b=0.75, k1=1.6):
        self.vectorizer = TfidfVectorizer(norm=None, analyzer=str.split, smooth_idf=False)
        self.b = b
        self.k1 = k1

    def fit(self, X):
        """ Fit IDF to documents X """
        self.vectorizer.fit(X)
        y = super(TfidfVectorizer, self.vectorizer).transform(X)
        self.avdl = y.sum(1).mean()

    def transform(self, q, X):
        """ Calculate BM25 between query q and documents X """
        b, k1, avdl = self.b, self.k1, self.avdl

        # apply CountVectorizer
        X = super(TfidfVectorizer, self.vectorizer).transform(X)
        len_X = X.sum(1).A1
        q, = super(TfidfVectorizer, self.vectorizer).transform([q])
        assert sparse.isspmatrix_csr(q)

        # convert to csc for better column slicing
        X = X.tocsc()[:, q.indices]
        denom = X + (k1 * (1 - b + b * len_X / avdl))[:, None]
        # idf(t) = log [ n / df(t) ] + 1 in sklearn, so it need to be coneverted
        # to idf(t) = log [ n / df(t) ] with minus 1
        idf = self.vectorizer._tfidf.idf_[None, q.indices] - 1.
        numer = X.multiply(np.broadcast_to(idf, X.shape)) * (k1 + 1)
        return (numer / denom).sum(1).A1


r1 = RFCPreprocessedWrapper(pathlib.Path("preprocessed_rfcs.jsonl"))
print("loading...")
rfcs = list(r1.iterate_over_rfcs())
words = [rfc.doc for rfc in rfcs]

print("counting...")
vectorizer = BM25()
vectorizer.fit(X=words)


def query(input_query) -> list[Rfc]:
    print("querying...")
    query_words = input_query.split()
    possible_rfcs: list[Rfc] = [rfc for rfc in rfcs
                                if any(word in rfc.words for word in query_words)]
    subset_words = [rfc.doc for rfc in possible_rfcs]
    result = vectorizer.transform(q=input_query, X=subset_words)

    top_k = 20
    best_matches = sorted([(x, i) for (i, x) in enumerate(result)])[-top_k:]
    rfc_matches = [possible_rfcs[i] for (x, i) in best_matches]
    pprint([(rfc.id, rfc.title) for rfc in rfc_matches])
    return rfc_matches


matches = query("gateway")
print("done")

import ipdb

ipdb.set_trace()
pass
