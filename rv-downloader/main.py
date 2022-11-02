import contextlib
from pathlib import Path
from string import Template
from types import TracebackType
from typing import Type
import pathlib

import diskcache
import requests
from bs4 import BeautifulSoup


class DownloadCache(contextlib.AbstractContextManager):
    cache_directory: Path
    inner: diskcache.Cache

    def __init__(self) -> None:
        self.cache_directory = pathlib.Path.home() / ".cache" / "rfc-viewer"
        self.inner = diskcache.Cache(directory=str(self.cache_directory))

    def __enter__(self):
        return self

    def __exit__(self,
                 __exc_type: Type[BaseException] | None,
                 __exc_value: BaseException | None,
                 __traceback: TracebackType | None) -> bool | None:
        self.inner.close()
        return True

    def __contains__(self, item):
        return item in self.inner

    def __getitem__(self, item):
        return self.inner[item]

    def __setitem__(self, key, value):
        self.inner[key] = value


class RfcGetter:
    cache: DownloadCache

    def __init__(self, cache: DownloadCache):
        self.cache = cache

    def get_rfc_info(self, rfc_id: str) -> str:
        cache_key: str = f"rfc_info:{rfc_id}"
        if cache_key in self.cache:
            return self.cache[cache_key]

        template: Template = Template("https://www.rfc-editor.org/info/rfc${rfc_id}")
        url = template.substitute(rfc_id=rfc_id)
        print(f"downloading {url}...")
        content = requests.get(url)
        content.raise_for_status()
        print(f"got 200 for {url}.")

        self.cache[cache_key] = content.text
        return self.cache[cache_key]


# This is a sample Python script.

# Press ⌃R to execute it or replace it with your code.
# Press Double ⇧ to search everywhere for classes, files, tool windows, actions, and settings.


def print_hi(name):
    # Use a breakpoint in the code line below to debug your script.
    print(f'Hi, {name}')  # Press ⌘F8 to toggle the breakpoint.
    pass


def main(rfc_getter: RfcGetter):
    print_hi('PyCharm')
    rfc_info: str = rfc_getter.get_rfc_info("8446")
    soup = BeautifulSoup(rfc_info, features="html.parser")
    import ipdb;
    ipdb.set_trace()
    pass


if __name__ == '__main__':
    with DownloadCache() as cache:
        rfc_getter: RfcGetter = RfcGetter(cache)
        main(rfc_getter)
