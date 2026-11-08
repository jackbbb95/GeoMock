# Address search — options and cost

Goal: type an address, move the map (and therefore the cursor) there.

## Why this is cheap to build

The cursor is already the camera. In `HomeScreen.kt`:

- `mainMarkerState.position = mockLocationLatLng ?: cameraPositionState.position.target`
  (line ~133) — the pin follows the camera centre.
- The "Set mock location" button reads `cameraPositionState.position.target` (line ~304).

So "move the cursor to an address" is only "animate the camera to a LatLng". No
changes to `MockLocationService`, `MockLocationManager` or the DataStore are
needed. The work is: a text field, a geocode call, one `CameraUpdate`.

## Options

| Approach | Cost | Free allowance | Setup | Suggestion quality | Notes |
| --- | --- | --- | --- | --- | --- |
| `android.location.Geocoder` (platform) | $0, no key, no SDK | n/a | none | Basic — full lookup per query, no ranked autocomplete | Backend varies by device/OEM; can return `SERVICE_NOT_AVAILABLE` or `isPresent() == false`; blocking call must run off the main thread (minSdk 26) |
| Places SDK for Android (New) — Autocomplete + Place Details | $2.83 / 1,000 autocomplete, $5.00 / 1,000 Details after caps | 10,000 + 10,000 per month | Enable "Places API (New)", reuse existing `MAPS_API_KEY` | Best — ranked, debounced, place names + addresses | Needs billing enabled on the Cloud project (already true for the Maps SDK key); adds `com.google.android.libraries.places:places` |
| Geocoding API (REST) | $5.00 / 1,000 after cap | 10,000 per month | Enable "Geocoding API", app-restrict the existing key | Good for full addresses, no autocomplete | Single HTTP call, no SDK; same key restrictions as the map key |
| OSM — Nominatim / Photon / self-hosted Pelias | $0 | Fair use | none (public) or a server | Photon is decent for autocomplete | Nominatim policy forbids client-side autocomplete and caps ~1 req/s; public instances are donation-funded and not for store-scale traffic |

Prices are Google's global list rates, first paid tier, checked 2026-11-03
(source: developers.google.com/maps/billing-and-pricing/pricing).

Note on the existing key: `MAPS_API_KEY` bills the **Maps SDK** SKU, which is
unlimited and $0. Adding geocoding moves part of the traffic onto a metered SKU
for the first time.

## Effort estimate

| Version | Rough size | Time |
| --- | --- | --- |
| Geocoder, submit-to-search, no suggestions | ~60–80 lines in `HomeScreen.kt` | an hour |
| Geocoder + debounced suggestions list | ~120 lines + a small ViewModel | half a day |
| Places Autocomplete + Details + session tokens | ~180 lines, new dependency, ViewModel | a day |

## Codebase gotchas

1. **Init race.** `LaunchedEffect(checkListState)` pans to the real location when
   permission resolves, guarded by `hasSetLocationOnInitOrUserInteracted`. A
   search must set that flag to `true`, or the camera can jump back afterwards.
2. **Mocking active.** When `mockLocationLatLng != null` the pin stays on the
   mock location while the camera moves. That is correct — searching must not
   silently move the mocked position — but the UI should say so, or the user
   will think the search failed.
3. **INTERNET permission** is currently inherited from the Firebase manifest
   merge, not declared in `app/src/main/AndroidManifest.xml`. It works, but
   declaring it explicitly is the honest thing once the app depends on network
   geocoding.
4. **Key restrictions.** The key is app-restricted; adding Geocoding API or
   Places API (New) to its allowed-API list in the Cloud console is a required
   step that is easy to forget (the map keeps working, the search returns
   `REQUEST_DENIED`).
5. **`secrets.properties`** already holds the key — nothing new to wire up.

## How forgiving is the platform Geocoder?

`getFromLocationName` is forward geocoding, not a search engine.

| Input | Result |
| --- | --- |
| House number + street + city | reliable |
| Street + city, city alone, city + country, postal code + country | reliable |
| Distinctive street name alone | usually works if biased to the visible area |
| Well-known landmark name ("Eiffel Tower") | sometimes — undocumented, do not rely on it |
| `123 Main St` with no city | usually fails or resolves to another continent |
| Partial word ("1600 Amphith") | always fails — no prefix matching |

**Bounding box is the main lever.** `getFromLocationName(name, maxResults, llLat, llLon, urLat, urLon)`
biases results to the visible map corners, which fixes most partial and ambiguous
queries for free. The app passes `cameraPositionState.projection?.visibleRegion?.latLngBounds`.

Implemented in `AddressGeocoder`: the bounded lookup runs first, and if it
returns nothing the lookup is retried without bounds. Testing proved why both
are needed — "Buckingham Palace" returned nothing while the map was over the
Atlantic, and resolved to `London SW1A 1AA, UK` on the unbiased retry. The bias
is a viewport, not a hint, so it can hide the world.

**Typos.** The backend does fuzzy matching: one transposed or missing letter in a
street name usually still resolves. Two errors, a wrong word, or a mangled city
name returns an empty list — no "did you mean", no error code. Treat `[]` as a
normal UI state with a message ("No match. Try adding a city or postal code"),
not as a failure.

**Do not build type-ahead on it.** Per-keystroke lookups return nothing for
partial input and feel broken. Use an explicit search action, then show up to 5
results as a list for disambiguation.

**Locale** affects ranking: results are ordered by device locale, so a US address
on a non-US device can rank oddly. The bounding box mostly neutralizes this.

**Escape hatch** if quality disappoints: Photon (free, autocomplete-friendly) as
a fallback when the platform returns nothing. Costs a network dependency and a
fair-use policy to respect, so only if the free path actually fails in practice.
