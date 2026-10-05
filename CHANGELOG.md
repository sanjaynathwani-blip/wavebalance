# Changelog

## 1.3.0 (2026-10-05)

- **Networks replaces Radar and AP Details.** On a wide window the radar, the band filters and
  a sortable table of every network sit beside the selected network's details. Search also
  matches the vendor. Shortcuts: N or 2 for Networks (R and A still work), and the numbers
  follow the navigation panel: 3 Optimizer, 4 Survey, 5 Speed Test.
- **Honest scan status** (from [Jesse Johnston](https://github.com/jessejamesjohnston), #7).
  When Android limits or refuses a scan, the results on screen are labelled as cached, with
  the time they were last measured, instead of looking fresh. The "retry in" wait counts down.
  Scan failures stay visible, and a permission error no longer clears the list.
- **Radar labels don't overlap.** Channel labels that would run into each other are left out;
  every channel keeps its tick.

## 1.2.0 (2026-10-05)

Fixes from [Jesse Johnston](https://github.com/jessejamesjohnston) (#1-#5).

- **The optimizer places wide channels correctly.** A 40, 80, 160 or 320 MHz candidate is scored
  on its real bonded block (channel 36 at 80 MHz covers 36-48, centred on 42), so a neighbour
  inside that block now counts against it. The cards, router steps and audit export show the
  centre channel. 2.4 GHz starts at 20 MHz, and a band you aren't connected on shows Unknown
  instead of a made-up current channel.
- **Analysis uses the whole scan.** Radar's search and band filter no longer change the
  Dashboard, the optimizer or the AP details.
- **Connection state stays in step.** The Connected badge follows network switches right away,
  simulated data no longer mixes with live scans, and only a change of access point on the same
  network counts as a roam.
- **Typing doesn't trigger shortcuts.** Letters and numbers typed in Radar's search stay in the
  field, and Ctrl, Alt and Shift combinations pass through.
- **Resizing keeps your place.** Selections such as the optimizer's band and width survive
  dragging the window between phone, rail and panel layouts.
- **Failed speed tests say so.** A test that ends during warm-up, or a malformed server from
  M-Lab, now fails with a message instead of showing 0 Mbps and an A+ grade.

## 1.1.0 (2026-10-04)

The first downloadable release.

- **Made for a desktop window.** A navigation panel with each screen's shortcut key, two-column
  pages on wide windows, a side rail on medium ones and a bottom bar at phone width. The window
  resizes freely and the layout follows.
- **Honest readings.** Security, spatial streams, protected management frames and 802.11k/v/r
  come from the access point's beacons; wide channels sit on their real centre frequency; vendor
  names come from the IEEE registry. Values Android doesn't report (noise floor, SNR) are no
  longer shown, and signal graphs only draw real samples.
- **Your own mesh isn't interference.** Every radio of your network (other bands, guest SSIDs,
  mesh nodes) is recognised and left out of the optimizer, the collision count and the radar.
- **A real speed test.** Download, upload, ping, jitter and bufferbloat measured against the
  nearest M-Lab server, one connection each way. M-Lab publishes results, including your IP
  address, as open data, so WaveBalance asks before the first test. Ping Only doesn't run a
  test, so nothing is published.
- Simulated data has a clear **SIMULATED** label, and the simulation shortcuts only work on it.
