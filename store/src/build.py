#!/usr/bin/env python3
"""store/src/*.svg から Google Play 用の PNG を生成する。

    python3 store/src/build.py        # リポジトリルートで実行

出力:
    store/icon-512.png                512x512 / RGBA(32bit, 全画素不透明) / <=1024KB
    store/feature-graphic-1024x500.png 1024x500 / RGB(24bit, アルファなし)

必要なもの: cairosvg, Pillow（導入手順は store/README.md）。
"""
import os
import sys

import cairosvg
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src")


def render(svg_name, png_name, width, height, mode):
    svg = os.path.join(SRC, svg_name)
    png = os.path.join(ROOT, png_name)
    cairosvg.svg2png(url=svg, write_to=png, output_width=width, output_height=height)
    # cairosvg は全画素が不透明だと RGB で書き出す。Play の要求する
    # ビット深度（アイコン=32bit / フィーチャー=24bit）に合わせて明示的に揃える。
    with Image.open(png) as im:
        im = im.convert(mode)
    im.save(png)
    return png


def report(png, expect_size, expect_mode):
    with Image.open(png) as im:
        size, mode = im.size, im.mode
        alpha = im.getchannel("A").getextrema() if mode == "RGBA" else None
    nbytes = os.path.getsize(png)
    ok = size == expect_size and mode == expect_mode
    print(
        "%-34s %s %-5s %7d bytes (%.1f KB)%s  %s"
        % (
            os.path.basename(png),
            "x".join(map(str, size)),
            mode,
            nbytes,
            nbytes / 1024,
            "" if alpha is None else "  alpha=%s" % (alpha,),
            "OK" if ok else "MISMATCH",
        )
    )
    return ok


def main():
    ok = True
    icon = render("icon.svg", "icon-512.png", 512, 512, "RGBA")
    ok &= report(icon, (512, 512), "RGBA")
    with Image.open(icon) as im:
        if im.getchannel("A").getextrema() != (255, 255):
            print("  NG: アイコンに透過画素がある（Play 用は背景色で全面を埋める）")
            ok = False
        if os.path.getsize(icon) > 1024 * 1024:
            print("  NG: アイコンが 1024KB を超えている")
            ok = False

    fg = render("feature-graphic.svg", "feature-graphic-1024x500.png", 1024, 500, "RGB")
    ok &= report(fg, (1024, 500), "RGB")
    # 文字が端で切れていないか（背景と異なる画素の範囲）を数値で確認する。
    with Image.open(fg) as im:
        # 白い図柄・文字だけを拾う（背景グラデーションと装飾円は R<=50）。
        ink = im.getchannel("R").point(lambda v: 255 if v > 90 else 0)
    bbox = ink.getbbox()
    print("  ink bbox (明るい画素の範囲) = %s / margin right = %d px" % (bbox, 1024 - bbox[2]))
    if bbox[0] < 16 or bbox[2] > 1024 - 16 or bbox[1] < 16 or bbox[3] > 500 - 16:
        print("  NG: 図柄か文字が端に接している")
        ok = False

    print("すべて OK" if ok else "問題あり")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
