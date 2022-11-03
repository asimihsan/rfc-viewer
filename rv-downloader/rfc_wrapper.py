import json
from pathlib import Path
from typing import Any


class RFCWrapper:
    def __init__(self, input_dir: Path):
        self.input_dir = input_dir

    def metadata_iterator(self):
        metadata_filename: Path
        for metadata_filename in self.input_dir.glob("*.json"):
            with open(metadata_filename) as f_in:
                metadata: dict[str, Any] = json.load(f_in)
            html_name: str = "%s.html" % (metadata["doc_id"].lower(),)
            with (self.input_dir / html_name).open() as f_in:
                metadata["$html"] = f_in.read()
            txt_name: str = "%s.txt" % (metadata["doc_id"].lower(),)
            with (self.input_dir / txt_name).open() as f_in:
                metadata["$txt"] = f_in.read()
            yield metadata
