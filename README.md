# MantraCounterApp
An offline japa counter for Android — swipe a mala, tap, or let a timer count for you. Daily logs, Custom background image, plain-text backups and more features. No network permission or any other except only one for vibration. 

**[Download the latest APK →](../../releases/latest)**

---

## What it does

**Three ways to count.** When you open a mantra it asks how you want to count this session:

- **Bead strand** — a vertical mala you swipe down to count and up to undo. The strand follows
  your finger and settles back when you lift it. The last bead of each round is a star, so you can
  see the mala closing before you reach it.
- **Tap** — the whole screen counts, wherever your thumb lands, with an undo button above.
- **Timer** — counts on its own at an interval you set, ringing a bell each time and closing with
  a chanted Om after your chosen number of malas.

A completed mala rings and vibrates for a second and a half. Both can be turned off.

**Everything else you'd want**

- Lifetime targets per mantra, with 10k / 1 lakh / 10 lakh / 1 crore presets
- Custom mala size — 27, 54, 108, 1008, or any number
- A background photo per mantra, shown behind the beads
- Deity field, favourites, and sorting by name, date added, deity or favourites
- Archive, with mantras filed into collapsible folders by deity
- Search across everything, carried from the main list into the archive
- Stats — every mantra's total and time spent, plus a bar chart of the current week
- History you can correct: change a day's count, move it to another date, or add a day you
  counted away from the phone
- Backup to a plain text file you can read, share or restore from
- Light and dark themes, and the screen stays awake while you're counting

## Privacy

The app declares **one permission: vibrate.** It has no network permission at all, so it cannot
send anything anywhere even by accident. No accounts, no analytics, no ads, no tracking. Your
counts live on your phone and go nowhere unless you export them yourself.

## Installing

Android will warn you about installing outside the Play Store — that is expected for any app
distributed this way.

1. Download the `.apk` from [Releases](../../releases/latest)
2. Open it and allow installation from this source when prompted
3. Android 8.0 or newer is required

**Updating:** install the new APK over the old one; your counts and history are kept.
[Obtainium](https://github.com/ImranR98/Obtainium) can watch this repository and update the app
for you automatically.

## Backing up

Settings → Backup writes every mantra — active and archived — with its full statistics and
day-by-day log to a plain `.txt` file you choose the location of, then offers to send a copy to
WhatsApp, Telegram, email or anywhere else. Restore reads that same file back and rebuilds
everything except the background photos, which are pictures rather than text.

The file is readable on its own. It will still make sense in ten years without this app.

## Licence

Copyright © 2026 Rishabh Dahiya

Licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE).

You are free to use, study, modify and share this software. If you distribute an app built from
it, whether modified or not, you must release your source under the same licence.

The name "Mantra Counter", the icon and the app's artwork are not covered by this licence. Please
use your own if you publish a fork.
