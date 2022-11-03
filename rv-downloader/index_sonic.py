#!/usr/bin/env python

import asyncio
import os
from pathlib import Path

import asonic

from rfc_wrapper import RFCWrapper

SCRIPT_DIR = Path(os.path.dirname(os.path.realpath(__file__)))


async def main():
    c = asonic.Client(host='127.0.0.1', port=1491)
    input_dir: Path = SCRIPT_DIR / "rsync"
    rfc_wrapper = RFCWrapper(input_dir)
    for data in rfc_wrapper.metadata_iterator():
        import ipdb
        ipdb.set_trace()
        pass
    # await c.push('rfc', 'default', )
    pass


if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    loop.run_until_complete(main())
