# libv2ray.aar provenance

This repository currently contains a locally patched binary Android archive
built from the 2dust AndroidLibXrayLite wrapper project.

- File: `libv2ray.aar`
- Upstream repository: `https://github.com/2dust/AndroidLibXrayLite`
- Upstream release: `v26.6.2`
- Upstream tag commit: `e194d22cf108ba960995fb8d4b2f594542e600a6`
- Local patch: `libv2ray-v26.6.2-cancellable.patch`
- Patch purpose: caller-bounded, explicitly cancellable `MeasureDelay` with
  generation-safe active-measurement ownership and cancellation before core
  shutdown.
- Build command:
  `gomobile bind -target=android -androidapi 24 -trimpath -ldflags='-s -w -buildid= -checklinkname=0' -o libv2ray.aar ./`
- Build toolchain: Go `1.26.3`, `golang.org/x/mobile`
  `v0.0.0-20260529142300-ecb4cd65260a`, Android NDK
  `29.0.14206865`.
- Size: `55897377` bytes
- SHA-256: `CF1D829174C12CD4781725DDDF30347A9231014F18684619208521EBD5B020D4`
- Local build timestamp: `2026-07-30`
- Archive entries include `classes.jar`, `proguard.txt`, geo assets, and JNI
  libraries for `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64`.
- Bundled `proguard.txt` keeps `go.**` and `libv2ray.**`.

Previous artifact:

- The unmodified AndroidLibXrayLite `v26.6.2` release asset was
  `https://github.com/2dust/AndroidLibXrayLite/releases/download/v26.6.2/libv2ray.aar`.
- Its SHA-256 was
  `367D6B2F74E62C974C61210C56802127812BE4C9410A83A6B8B6CAC765A7595E`
  and its size was `56764555` bytes.
- The 2026-05-02 import used AndroidLibXrayLite `v26.3.23`.
- Previous SHA-256:
  `1D4F5501B01DAF909DEBA48954EB0006359652D1AA903B5E78958E1FCE5BC50E`.
- Previous size: `55670069` bytes.

Source status:

- This is the same third-party wrapper family used by v2rayNG-style Android
  clients. It is not the official XTLS/Xray-core release artifact.
- Official XTLS/Xray-core publishes Android `xray` ZIP binaries.
- Official XTLS/libXray is a source-built wrapper path and uses a different
  Android API from the current `libv2ray.*` integration.
- Do not replace this file with unrelated third-party AARs. For future updates,
  use a pinned AndroidLibXrayLite release or migrate deliberately to
  XTLS/libXray with controller/probe adapter changes.
