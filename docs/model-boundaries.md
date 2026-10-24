# Model boundaries and state ownership

The original character class was a Gson response, a Room entity, a domain value, and a Parcelable
navigation payload. The split gives changes to each representation a clear owner. Immutable models
help keep values predictable, but request ownership, cancellation, and consistent screen state still
need their own handling.

## What each model owns

| Model | Owner | Purpose |
| --- | --- | --- |
| `CharacterDto`, `CharacterPageDto`, `PageInfoDto` | Data | Remote field names and pagination links |
| `CharacterEntity` | Data | The saved-character table, columns, and primary key |
| `Character`, `GenderFilter` | Domain | Character information and filter choices |
| `CharacterUiModel` | Presentation | Display labels, image URL, name, and first-seen date |
| `CharacterFilters` | Presentation | Restorable browser inputs |
| `DetailsUiState`, `SavedCharactersUiState` | Presentation | Content, progress, failures, and results awaiting acknowledgement |

Data records do not import domain or presentation models. Domain models have no Android, JSON, Room,
or presentation annotations. UI model files do not import data or domain models. The mapper files
import the representations they convert.

## Where conversion happens

`RMPagingSource` returns DTO pages. `DefaultCharacterRepository` maps those pages to domain
characters and maps Room entities at the same boundary. It converts domain characters to entities
when saving. The API gender string is also selected in the data implementation.

`RickAndMortyViewModel` maps each domain page to UI characters before `cachedIn(viewModelScope)`.
`SavedViewModel` maps the observed saved list before publishing screen state. `DetailsViewModel`
maps the loaded character once for display. Formatting an unknown status or trimming a timestamp for
display never changes the stored value.

Do not reconstruct a domain character from the displayed name, status, and date. The UI deliberately
omits fields such as the original timestamp, type, and source URL. Saving and undo use the original
domain object held by the ViewModel.

The repository contract uses `PagingData<Character>`. This small Views sample deliberately shares
the Paging library across the repository boundary rather than adding a custom paging abstraction.
Domain models remain plain Kotlin. A project requiring a library-independent domain contract would
need a separate pagination contract.

## Navigation and profile loading

Both character grids pass `DetailsViewModel.CHARACTER_ID` in the details intent. `SavedStateHandle`
makes that ID available when Android recreates the profile. No entity, DTO, or complete character
object is passed through navigation.

The repository checks Room first, then a synchronized cache of up to 100 domain characters, then the
API. Displayed pages populate that cache. Saved records therefore work offline, and recently
displayed profiles can open without another request. An uncached, unsaved profile still needs the
network. Use database-backed content or another explicit cache policy when offline access must
survive a process restart.

A profile load failure produces a retry state. The ViewModel cancels the previous load before
retrying. Activities render only UI values and forward user actions.

## Write state and undo

The old save flow launched a write and immediately showed a success dialog. It could report success
before Room completed the operation. The new details state sets `isSaving` before starting the job,
ignores repeated save taps, and publishes success or failure after the repository call completes.
Cancellation is rethrown instead of being reported as a failed save.

The collection observes Room without an artificial delay. Loading, content, update progress, and
errors are published in one state value. Collection writes are serialized so a second action cannot
overwrite the pending undo data while a write is in progress.

After deletion succeeds, the ViewModel retains the deleted domain character for undo. The screen
receives a removal message and calls `undoDelete(id)` when requested. The Activity never rebuilds a
record from its card text. The snackbar carries the deleted ID, and the ViewModel checks it before
undoing a removal. An old snackbar cannot restore a different, more recently deleted character.
Clearing the collection invalidates the previous undo.

Result messages remain in state until a started Activity displays and acknowledges them. They
survive Activity recreation while their ViewModel is alive; transient save results and pending undo
data are not persisted across process restarts.

## Room compatibility

The database remains version 1. `CharacterEntity` keeps the `characters` table, the same column
names and types, and the same auto-generated integer primary-key definition. API IDs are supplied
explicitly for saves, so saving the same character replaces its existing record.

Renaming the Kotlin representation does not change that schema. Destructive migration fallback is
disabled. Future schema changes need a version increase and an explicit migration rather than
erasing the collection.

## Verification

Run `./gradlew testDebugUnitTest lintDebug assembleDebug`.

The tests exercise repository field mapping and lookup, display formatting, page boundaries, delayed
saves, duplicate taps, failures, cancellation, retry, and undo. The emulator checks updated the
existing installation without clearing app data, then verified:

- The existing saved collection still opens with the unchanged Room schema.
- A saved profile opens by ID with the network disconnected.
- A recently browsed, unsaved profile opens offline from the process cache.
- Swiping a saved character and tapping Undo restores the record in the collection.

Network settings were restored after the offline checks. These checks are narrower than a full
device, accessibility, and process-restoration test suite.
