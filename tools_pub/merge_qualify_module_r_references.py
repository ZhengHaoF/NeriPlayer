"""Rewrite bare `R.<type>.<key>` references in :app sources to the owning module's R class.

android.nonTransitiveRClass=true means app/R only contains app-local resources, so every
key the upstream refactor moved into :common (or another module) needs a qualified R.
Only rewrites keys that are actually absent from app/res and present in a module's res.
"""
import os
import re
import sys
from collections import defaultdict

APP_RES = "app/src/main/res"
MODULE_RES_ROOTS = [
    ("modules/common/src/main/res", "CoreCommonR", "moe.ouom.neriplayer.common.R"),
    ("modules/local/src/main/res", "LocalR", "moe.ouom.neriplayer.local.R"),
    ("modules/platform/src/main/res", "PlatformR", "moe.ouom.neriplayer.platform.R"),
    ("modules/model/src/main/res", "ModelR", "moe.ouom.neriplayer.model.R"),
]

TYPE_DIRS = {
    "drawable": re.compile(r"^drawable"),
    "string": re.compile(r"^values.*"),
    "color": re.compile(r"^values.*"),
    "dimen": re.compile(r"^values.*"),
    "plurals": re.compile(r"^values.*"),
    "array": re.compile(r"^values.*"),
    "layout": re.compile(r"^layout"),
    "raw": re.compile(r"^raw"),
    "mipmap": re.compile(r"^mipmap"),
}

VALUE_RE = re.compile(r'<(string|color|dimen|plurals|string-array|integer-array|array)\s+name="([^"]+)"')
USE_RE = re.compile(r'(?<![\w.])R\.(string|drawable|color|dimen|plurals|array|layout|raw|mipmap)\.([A-Za-z0-9_]+)')


def names_in(res_root):
    names = set()
    if not os.path.isdir(res_root):
        return names
    for folder in os.listdir(res_root):
        path = os.path.join(res_root, folder)
        if not os.path.isdir(path):
            continue
        for entry in os.listdir(path):
            stem = entry.split(".")[0]
            if folder.startswith("values"):
                with open(os.path.join(path, entry), encoding="utf-8", errors="ignore") as handle:
                    names.update(match.group(2) for match in VALUE_RE.finditer(handle.read()))
            elif re.match(r"^(drawable|layout|mipmap|raw|anim)", folder):
                names.add(stem)
    return names


def main():
    app_names = names_in(APP_RES)
    module_index = {}
    for res_root, alias, r_class in MODULE_RES_ROOTS:
        if not os.path.isdir(res_root):
            continue
        for name in names_in(res_root):
            module_index.setdefault(name, (alias, r_class))

    changed_files = 0
    rewrites = 0
    unresolved = defaultdict(list)

    for root, _, files in os.walk("app/src/main"):
        for fname in files:
            if not fname.endswith(".kt"):
                continue
            path = os.path.join(root, fname)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if "R." not in text:
                continue
            needed_alias = {}

            def replace(match):
                nonlocal rewrites
                kind, key = match.group(1), match.group(2)
                if key in app_names:
                    return match.group(0)
                owner = module_index.get(key)
                if owner is None:
                    unresolved[key].append(path)
                    return match.group(0)
                alias, r_class = owner
                needed_alias[alias] = r_class
                rewrites += 1
                return f"{alias}.{kind}.{key}"

            updated = USE_RE.sub(replace, text)
            if needed_alias:
                import_lines = "\n".join(
                    f"import {r_class} as {alias}" for alias, r_class in sorted(needed_alias.items())
                )
                already = {line for line in updated.splitlines() if line.startswith("import ")}
                to_add = [
                    line for line in import_lines.splitlines() if line not in already
                ]
                if to_add:
                    first_import = updated.index("import ")
                    updated = updated[:first_import] + "\n".join(to_add) + "\n" + updated[first_import:]
                with open(path, "w", encoding="utf-8", newline="\n") as handle:
                    handle.write(updated)
                changed_files += 1

    print(f"rewrote {rewrites} references across {changed_files} files")
    if unresolved:
        print(f"\n{len(unresolved)} keys referenced by app but found in NO known res root:")
        for key, paths in sorted(unresolved.items())[:40]:
            print(f"  {key}  <- {paths[0]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
