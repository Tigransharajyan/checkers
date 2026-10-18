# Locally served dependencies

- Lucide 0.468.0, ISC license: official icon definitions and `createIcons` renderer, tree-shaken to the 41 icons referenced by this application. Rebuild with `scripts/build-icons.cjs` as documented in that script; no Node tooling is required to build or run the Spring application.
- SockJS client 1.6.1, MIT license: unmodified `dist/sockjs.min.js`.
- stompjs 2.3.3, Apache License 2.0: unmodified `lib/stomp.min.js`.

Files were taken from the corresponding versioned npm packages. License texts are included next to the bundles. These are the same versions previously requested from jsDelivr; self-hosting removes third-party network dependencies during page load.
