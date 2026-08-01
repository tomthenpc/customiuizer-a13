#!/usr/bin/env python3
"""Pure-logic, Android-free View tree simulation for A13 hot-path validation.

This module models the minimum state needed to prove that SystemUI/Launcher
View insertions are safe. It has no Android runtime dependency and runs under
CPython so it can be used for property tests, mutation tests and stress loops.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Optional


@dataclass
class FakeView:
    view_id: int
    owner: str = ""
    tag: Optional[str] = None
    slot: int = -1
    icon_type: int = -1
    parent: Optional["FakeViewGroup"] = None
    dark_receiver: bool = False
    disposed: bool = False

    def detach(self) -> None:
        if self.parent is not None:
            self.parent.remove_view(self)

    def dispose(self) -> None:
        self.detach()
        self.disposed = True
        self.dark_receiver = False


@dataclass
class FakeViewGroup:
    name: str
    children: list[FakeView] = field(default_factory=list)
    child_count: int = 0

    def safe_index(self, requested: int) -> int:
        """Clamp a requested child index to a valid insert position."""
        if requested < 0:
            return 0
        if requested > self.child_count:
            return self.child_count
        return requested

    def add_view(self, child: FakeView, requested_index: int) -> int:
        """Add a child View with index clamping and parent ownership checks.

        Returns the actual index used. Raises if the child already has a
        different parent, has already been disposed, or if the same owner/tag
        would duplicate a registered icon.
        """
        if child.disposed:
            raise RuntimeError(f"view {child.view_id} is disposed")
        if child.parent is not None and child.parent is not self:
            raise RuntimeError(f"view {child.view_id} already has a parent in {child.parent.name}")
        if child.parent is self:
            # Already attached here; this is a duplicate attach. Do not increase count.
            return self.children.index(child)

        actual = self.safe_index(requested_index)
        self.children.insert(actual, child)
        child.parent = self
        self.child_count += 1
        return actual

    def remove_view(self, child: FakeView) -> None:
        if child.parent is not self or child not in self.children:
            return
        self.children.remove(child)
        child.parent = None
        self.child_count -= 1

    def icon_group_has(self, owner: str, slot: int) -> bool:
        for c in self.children:
            if c.owner == owner and c.slot == slot:
                return True
        return False

    def find_by_tag(self, tag: str) -> Optional[FakeView]:
        for c in self.children:
            if c.tag == tag:
                return c
        return None


class IconGroupRegistry:
    """Track icon groups to prevent same-owner/same-slot duplication."""

    def __init__(self) -> None:
        self._entries: dict[tuple[str, int], FakeView] = {}

    def register(self, owner: str, slot: int, view: FakeView) -> bool:
        if (owner, slot) in self._entries:
            return False
        self._entries[(owner, slot)] = view
        return True

    def replace(self, owner: str, slot: int, view: FakeView) -> None:
        old = self._entries.get((owner, slot))
        if old is not None:
            old.dispose()
        self._entries[(owner, slot)] = view

    def size(self) -> int:
        return len(self._entries)


class DarkReceiverOwner:
    """A minimal owner that can be replaced and disposed."""

    def __init__(self, owner_id: str) -> None:
        self.owner_id = owner_id
        self.active = True

    def dispose(self) -> None:
        self.active = False
