"""Lock-screen album-art large-object lifecycle invariants."""

import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parent.parent.parent
CONTROLLER_PATH = (
    REPO_ROOT
    / "app/src/main/java/tv/withaibuild/customiuizer/mods/utils/LockScreenAlbumArtController.kt"
)


class AlbumArtLifecycleInvariant(unittest.TestCase):
    def test_detached_owner_releases_rendered_frame_but_keeps_source(self) -> None:
        source = CONTROLLER_PATH.read_text(encoding="utf-8")
        detach = source.split("override fun onViewDetachedFromWindow", 1)[1].split(
            "ownerRef = WeakReference(view)", 1
        )[0]
        release = source.split("private fun releaseRenderedArtwork()", 1)[1].split(
            "private fun applyBackground", 1
        )[0]

        self.assertIn("cancelWork()", detach)
        self.assertIn("releaseRenderedArtwork()", detach)
        self.assertIn("clearAppliedBackground()", release)
        self.assertIn("clearCache()", release)
        self.assertIn('"mAlbumArt", null', release)
        self.assertNotIn("pendingSource = null", release)
        self.assertNotIn('"mAlbumArtSource", null', release)


if __name__ == "__main__":
    unittest.main()
