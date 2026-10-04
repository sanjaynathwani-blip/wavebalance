# Changelog

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
