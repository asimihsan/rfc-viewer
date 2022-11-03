#!/usr/bin/env python
import operator
import os
import json
from pathlib import Path
from typing import Any

SCRIPT_DIR = Path(os.path.dirname(os.path.realpath(__file__)))


def main():
    input_dir: Path = SCRIPT_DIR / "rsync"
    output_file: Path = SCRIPT_DIR / "rfc-stork.json"

    output: dict[str, Any] = {
        "input": {
            "base_directory": str(input_dir),
            "html_selector": "body",
            "url_prefix": "https://www.rfc-editor.org/rfc/",
            "files": [],
        },
        "output": {
            # "displayed_results_count": 100,
            # "excerpt_buffer": 8,
            # "excerpts_per_result": 20,
            "save_nearest_html_id": True,
        }}

    metadata_filename: Path
    for metadata_filename in input_dir.glob("*.json"):
        with open(metadata_filename) as f_in:
            metadata: dict[str, Any] = json.load(f_in)
        html_name: str = "%s.html" % (metadata["doc_id"].lower(),)

        file_datum: dict[str, str] = {
            "path": html_name,
            "url": html_name,
            "title": metadata["title"].strip()
        }
        output["input"]["files"].append(file_datum)

    output["input"]["files"].sort(key=operator.itemgetter("path"), reverse=True)
    with output_file.open("w") as f_out:
        json.dump(output, f_out, sort_keys=True, indent=4)


if __name__ == "__main__":
    main()
