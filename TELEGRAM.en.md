🗺 Google Maps for BYD: moving to Automotive

We are ending support for our phone edition. Development continues with Automotive, the edition for Android head units.

Build your own APK:
1. Install Morphe Manager.
2. Use a clean original Maps Automotive 26.09.280001.E APK, not installed phone Maps or an already patched APK.
3. Add our source below, patch Maps and Save.
4. Install MicroG-RE 7.2.1 and your new APK.

Phone Maps can stay installed. Replacing old project-signed Automotive with your own key requires uninstalling/reinstalling it, which clears its local data. Back up your key for later updates. Keep old microG if other apps need it.

Morphe: https://github.com/MorpheApp/morphe-manager/releases
Add our TEST patch to Morphe: https://morphe.software/add-source?github=romanchukg-cloud/maps-automotive-for-byd-patches&name=Maps%20Automotive%20for%20BYD%20%28TEST%29
Full steps and exact input: https://github.com/romanchukg-cloud/maps-automotive-for-byd-patches/blob/main/MIGRATION.md
