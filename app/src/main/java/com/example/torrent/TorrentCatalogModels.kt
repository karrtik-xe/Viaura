/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Torrent source parsing / metadata models (download only, no playback). Based on mpvRx (AGPL-3.0).
 */

package com.example.torrent

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class TorrentFileItem(
  val index: Int,
  val path: String,
  var name: String,
  val size: Long,
  val mimeType: String,
) {
  init {
    name = normalizeTorrentDisplayName(name)
  }
}

/** Metadata-only result used by the torrent file picker (download only). */
data class TorrentCatalog(
  val preparationId: String,
  /** A durable source. This is never a content grant or loopback proxy URL. */
  val source: String,
  val infoHash: String,
  var torrentName: String,
  val files: List<TorrentFileItem>,
) {
  init {
    torrentName = normalizeTorrentDisplayName(torrentName)
  }
}

data class ParsedMagnet(
  val infoHash: String,
  val cleanMagnetUri: String,
  val fileIdx: Int?,
  val displayName: String?,
  val trackers: List<String>,
)

class TorrentException(
  message: String,
  cause: Throwable? = null,
) : Exception(message, cause)

val DEFAULT_TORRENT_TRACKERS =
  listOf(
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://open.stealth.si:80/announce",
    "udp://tracker.torrent.eu.org:451/announce",
    "udp://explodie.org:6969/announce",
    "udp://tracker.openbittorrent.com:6969/announce",
    "udp://p4p.arenabg.com:1337/announce",
    "https://tracker.opentrackr.org:443/announce",
  )
