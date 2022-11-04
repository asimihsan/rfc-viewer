#!/usr/bin/env python

import concurrent.futures
import json
import os
from pathlib import Path

from rfc_wrapper import RFCWrapper, process_rfc

SCRIPT_DIR = Path(os.path.dirname(os.path.realpath(__file__)))


def main():
    input_dir: Path = SCRIPT_DIR / "rsync"
    output_file: Path = SCRIPT_DIR / "preprocessed_rfcs.jsonl"
    rfc_wrapper = RFCWrapper(input_dir)

    metadata_filenames = sorted(rfc_wrapper.get_metadata_filenames())
    futures = []
    num_processes = max(os.cpu_count() - 2, 1)
    with output_file.open("w") as f_out:
        with concurrent.futures.ProcessPoolExecutor(max_workers=num_processes) as executor:
            for filename in metadata_filenames:
                future = executor.submit(process_rfc, filename)
                futures.append(future)
            for i, completed in enumerate(concurrent.futures.as_completed(futures)):
                (metadata_filename, result) = completed.result()
                print("%s finished from pool" % (metadata_filename,))
                if result is not None:
                    line = json.dumps(result, sort_keys=True)
                    f_out.write(line)
                    f_out.write(os.linesep)
                if i % 1000 == 0:
                    f_out.flush()


if __name__ == "__main__":
    main()
