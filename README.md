# Bakaláři Morphe Patches

Morphe patches for **cz.bakalari.mobile** (Bakaláři OnLine) that disable sponsored ad pop-ups.

## Patches

### Disable sponsored ads
Blocks sponsored ad pop-ups by targeting the ad-serving CDN URLs directly:
- `campaign.bakalari.cz`
- `campaign-dev.bakalari.cz`
- `netglade.cz`

This approach is **app-version agnostic** — it targets stable backend infrastructure endpoints rather than obfuscated in-app code, so it continues to work across app releases.

## Usage

These patches are consumed by the [Morphe](https://github.com/MorpheApp/morphe-patcher) manager app. Add this repository as a patch source in Morphe to apply the patches to `cz.bakalari.mobile`.

## License

GPLv3 — see the NOTICE file.
