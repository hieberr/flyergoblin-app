# App design spec

## Domain

# Event Details

Event details to extract from flyers and save in th event list.

- Event Name (required)
- Event start date (required)
- Event start time (optional)
- Venue (optional)
    - The name of the venue that the event takes place
- Event url (optional)
    - The main event url provided on the flyer.
- Artists (List) (optional)
    - List of bands, djs, or artists performing.

## UI

### Events Screen

- Root screen is the top level 'Flyer' screen of the app.
- Displays a list of saved events. List can be sorted by either the date it was added, or by the date of the event itself.
- Provides a button for users to add a new event. Brings up the `AddEvent` screen
- Tapping an event in the list brings up the `EventDetail` screen.

### EventDetail screen

- Displays all of the saved details for an event.
- User can edit any of the fields.
- Provides a button to delete the event.
- Event details
- When an artist is clicked on opens the ArtistDetail screen

### EditEvent screen

- Similar layout to EventDetailScreen but allows for editing fields.
- Provides a button to browse for a file (this can be a standard file picker)
- Once the file is provided it is sent off to the remote ai agent to process. While waiting for processing a wait spinner is shown as an overlay until it completes.
- When the processing completes
    - If successful, create an event, save it, and switch to the EventDetail screen for that event.
    - If not successful, display the error and remain on the AddEve t screen.
- Edit image button triggers an EditImageScreen to appear which allows the user to crop the image.

### ArtistDetail screen

- Shows the artist's SoundCloud profile that we found by searching
- Tapping on the SoundCloud profile button brings up the SoundCloudProfileSelection screen which allows the user to select a different SoundCloud profile from our list of profiles saved from the search request.
- Shows a button which opens the SoundCloud profile url externally.
- Shows a webview which contains SoundCloud track player widgets.
