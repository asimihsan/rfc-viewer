import base64
import json
import string
from pathlib import Path
from typing import Any, Optional
from dataclasses import dataclass, field

import chardet
import spacy
import zstandard
from bs4 import BeautifulSoup

nlp = spacy.load("en_core_web_trf")


@dataclass
class Rfc:
    id: str
    title: str = field(compare=False)
    abstract: str = field(compare=False)
    data: dict = field(compare=False)
    words: list[str] = field(compare=False)
    doc: str = field(compare=False)


class RFCPreprocessedWrapper:
    data: dict[str, dict]

    def __init__(self, filepath: Path):
        self.data = {}
        with filepath.open() as f_in:
            for line in f_in:
                datum = json.loads(line)
                self.data[datum["doc_id"].lower()] = datum

    def iterate_over_rfcs(self):
        for rfc_id in self.data.keys():
            yield self.get_rfc(rfc_id)

    def get_rfc(self, rfc_id: str) -> Rfc:
        datum = self.data[rfc_id]
        if "$html" in datum:
            datum["$html"] = json_decompress(datum["$html"])
        datum["words"] = json_decompress(datum["words"])
        return Rfc(id=datum["doc_id"], title=datum["title"], abstract=datum["abstract"], data=datum,
                   words=datum["words"], doc=" ".join(datum["words"]))


class RFCWrapper:
    def __init__(self, input_dir: Path):
        self.input_dir = input_dir

    def get_metadata_filenames(self):
        return self.input_dir.glob("*.json")


def process_rfc(metadata_filename: Path) -> tuple[Path, Optional[dict]]:
    print("process_rfc start %s" % (metadata_filename,))
    with open(metadata_filename) as f_in:
        metadata: dict[str, Any] = json.load(f_in)

    if metadata["pub_status"] == "NOT ISSUED":
        return metadata_filename, None

    tokenizer = nlp.tokenizer

    txt_path: Path = metadata_filename.with_suffix(".txt")
    html_path: Path = metadata_filename.with_suffix(".html")
    use_txt = False
    content: Optional[str] = None
    if html_path.exists():
        print("process_rfc %s using HTML" % (metadata_filename,))
        with html_path.open("rb") as f_in:
            html_content: bytes = f_in.read()
        detected = chardet.detect(html_content)
        if detected["encoding"] is None:
            use_txt = True
        else:
            try:
                html_content: str = html_content.decode(encoding=detected["encoding"])
            except UnicodeDecodeError:
                use_txt = True
            else:
                metadata["$html"] = json_compress(html_content)
                soup = BeautifulSoup(html_content, features="html.parser")
                content = ". ".join(soup.strings)
    if use_txt and txt_path.exists():
        print("process_rfc %s using TXT" % (metadata_filename,))
        with txt_path.open("rb") as f_in:
            txt_content: bytes = f_in.read()
        detected = chardet.detect(txt_content)
        if detected["encoding"] is not None:
            try:
                txt_content: str = txt_content.decode(encoding=detected["encoding"])
            except UnicodeDecodeError:
                pass
            else:
                metadata["$txt"] = json_compress(txt_content)
                content = txt_content
    if content is None:
        print("No content available for %s" % (metadata_filename,))
        return metadata_filename, None

    doc = tokenizer(content)
    words = (token.text.strip() for token in doc)
    words = (word for word in words if len(word) > 0)
    words = (word for word in words if not all(x in string.punctuation for x in word))
    words = list(words)
    metadata["words"] = json_compress(words)

    print("process_rfc finish %s" % (metadata_filename,))

    return metadata_filename, metadata


def json_compress(input_bytes) -> str:
    serialized: bytes = json.dumps(input_bytes).encode(encoding='utf-8')
    compressed: bytes = zstandard.compress(serialized, level=19)
    return base64.b64encode(compressed).decode('ascii')


def json_decompress(input_str: str):
    decoded: bytes = base64.b64decode(input_str)
    decompressed: bytes = zstandard.decompress(decoded)
    return json.loads(decompressed)
