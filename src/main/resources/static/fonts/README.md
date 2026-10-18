# Self-hosted fonts

The existing Inter and Noto Sans Armenian variable font families are preserved. Files are WOFF2 subsets returned by the Google Fonts CSS API for `Inter:wght@400;500;600;700;800;900` and `Noto Sans Armenian:wght@400;500;600;700;800;900`, downloaded on 2026-10-18. The CSS API was requested with a modern Chromium user agent to obtain WOFF2, not a TTF fallback.

`/css/fonts.css` retains the corresponding weights and Unicode ranges, so a browser downloads only the subsets used by the page. `font-display: swap` keeps text visible. The SIL Open Font License texts are included as `inter-OFL.txt` and `notosansarmenian-OFL.txt`.

No runtime request to Google Fonts or a third-party font CDN is needed.
