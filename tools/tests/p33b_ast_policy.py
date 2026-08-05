from __future__ import annotations

import ast
import re
from dataclasses import dataclass


BUILD_REGISTRY_MODULE_PATHS = {
    "build_legacy_exception_registry",
    "tools.build_legacy_exception_registry",
}
FUNCTION_NAME = "build_registry"


@dataclass(frozen=True)
class Violation:
    class_name: str | None
    function_name: str | None
    line: int
    col: int
    message: str


def _attribute_chain(node: ast.AST) -> str | None:
    parts = []
    current = node
    while isinstance(current, ast.Attribute):
        parts.append(current.attr)
        current = current.value
    if isinstance(current, ast.Name):
        parts.append(current.id)
        return ".".join(reversed(parts))
    return None


def _getattr_target(call: ast.Call) -> ast.AST | None:
    """If call is getattr(x, 'build_registry'), return x."""
    func = call.func
    if isinstance(func, ast.Name) and func.id == "getattr":
        if len(call.args) >= 2:
            name_arg = call.args[1]
            if isinstance(name_arg, ast.Constant) and name_arg.value == FUNCTION_NAME:
                return call.args[0]
    if isinstance(func, ast.Attribute) and func.attr == "getattr":
        if len(call.args) >= 2:
            name_arg = call.args[1]
            if isinstance(name_arg, ast.Constant) and name_arg.value == FUNCTION_NAME:
                return call.args[0]
    return None


def _partial_target(call: ast.Call) -> ast.AST | None:
    """If call is partial(x, ...), return x."""
    func = call.func
    if isinstance(func, ast.Name) and func.id == "partial":
        if call.args:
            return call.args[0]
    if isinstance(func, ast.Attribute) and func.attr == "partial":
        if call.args:
            return call.args[0]
    return None


class _AliasCollector(ast.NodeVisitor):
    """Collect local names that refer to the build_registry callable."""

    def __init__(self) -> None:
        self.function_aliases: dict[str, str] = {}
        self.module_aliases: dict[str, str] = {}
        self.package_aliases: dict[str, str] = {}
        self.assignment_aliases: dict[int, str] = {}

    def visit_Import(self, node: ast.Import) -> None:
        for alias in node.names:
            full = alias.name
            asname = alias.asname if alias.asname else full.split(".")[0]
            if full.endswith("build_legacy_exception_registry"):
                if alias.asname:
                    self.module_aliases[asname] = full
                else:
                    # import tools.build_legacy_exception_registry -> local name is top package
                    self.package_aliases[asname] = full

    def visit_ImportFrom(self, node: ast.ImportFrom) -> None:
        if node.module is None:
            return
        if not node.module.endswith("build_legacy_exception_registry"):
            return
        for alias in node.names:
            if alias.name == FUNCTION_NAME:
                asname = alias.asname if alias.asname else alias.name
                self.function_aliases[asname] = f"{node.module}.{alias.name}"

    def _is_module_ref(self, node: ast.AST) -> bool:
        chain = _attribute_chain(node)
        if chain is None:
            return False
        # Direct module attribute: e.g. builder.build_registry not yet, here just module
        # We accept a reference to the module itself only through a known alias.
        if isinstance(node, ast.Name) and node.id in self.module_aliases:
            return True
        if isinstance(node, ast.Name) and node.id in self.package_aliases:
            return True
        # import build_legacy_exception_registry; build_legacy_exception_registry (as package)
        if chain in BUILD_REGISTRY_MODULE_PATHS:
            return True
        if chain in self.module_aliases.values():
            return True
        if chain in self.package_aliases.values():
            return True
        return False

    def _is_build_registry_ref(self, node: ast.AST) -> bool:
        if isinstance(node, ast.Name):
            return node.id in self.function_aliases

        chain = _attribute_chain(node)
        if chain:
            if chain == FUNCTION_NAME:
                # bare build_registry as function name
                return True
            if not chain.endswith(f".{FUNCTION_NAME}"):
                return False
            prefix = chain[: -len(FUNCTION_NAME) - 1]
            if prefix in self.module_aliases:
                return True
            if prefix in self.module_aliases.values():
                return True
            if prefix in self.package_aliases.values():
                return True
            if prefix in BUILD_REGISTRY_MODULE_PATHS:
                return True
        return False

    def _value_is_callable(self, node: ast.AST) -> bool:
        if self._is_build_registry_ref(node):
            return True
        if isinstance(node, ast.Call):
            if _getattr_target(node) is not None and self._is_module_ref(_getattr_target(node)):
                return True
            partial_arg = _partial_target(node)
            if partial_arg is not None and self._is_build_registry_ref(partial_arg):
                return True
        return False

    def visit_Assign(self, node: ast.Assign) -> None:
        if not self._value_is_callable(node.value):
            return
        for target in node.targets:
            if isinstance(target, ast.Name):
                self.function_aliases[target.id] = "<alias>"

    def visit_NamedExpr(self, node: ast.NamedExpr) -> None:
        if self._value_is_callable(node.value) and isinstance(node.target, ast.Name):
            self.function_aliases[node.target.id] = "<alias>"


def _build_parent_map(tree: ast.AST) -> dict[ast.AST, ast.AST]:
    parents: dict[ast.AST, ast.AST] = {}
    for parent in ast.walk(tree):
        for child in ast.iter_child_nodes(parent):
            parents[child] = parent
    return parents


def _enclosing_class_name(node: ast.AST, parents: dict[ast.AST, ast.AST]) -> str | None:
    current = parents.get(node)
    while current is not None:
        if isinstance(current, ast.ClassDef):
            return current.name
        current = parents.get(current)
    return None


def _enclosing_function_name(node: ast.AST, parents: dict[ast.AST, ast.AST]) -> str | None:
    current = parents.get(node)
    while current is not None:
        if isinstance(current, (ast.FunctionDef, ast.AsyncFunctionDef)):
            return current.name
        current = parents.get(current)
    return None


def inject_method(source: str, class_name: str, method_source: str) -> str:
    """Return source with a new method inserted into the named class."""
    tree = ast.parse(source)
    for node in ast.walk(tree):
        if isinstance(node, ast.ClassDef) and node.name == class_name:
            method_tree = ast.parse(method_source)
            if method_tree.body and isinstance(method_tree.body[0], (ast.FunctionDef, ast.AsyncFunctionDef)):
                node.body.append(method_tree.body[0])
            else:
                raise ValueError("method_source must contain a single function definition")
            break
    else:
        raise ValueError(f"class {class_name!r} not found in source")
    return ast.unparse(tree)


def find_build_registry_violations(
    source: str,
    allowed_classes: set[str],
    module_name: str = "module",
) -> list[Violation]:
    """Return violations for any reference to build_registry outside allowlisted classes."""
    try:
        tree = ast.parse(source)
    except SyntaxError as e:
        raise SyntaxError(f"failed to parse {module_name}: {e}") from e

    parents = _build_parent_map(tree)

    # Collect aliases in an order that lets us add assignment aliases.
    collector = _AliasCollector()
    collector.visit(tree)

    function_aliases = collector.function_aliases
    module_aliases = collector.module_aliases
    package_aliases = collector.package_aliases

    def is_build_registry_ref(node: ast.AST) -> bool:
        if isinstance(node, ast.Name):
            return node.id == FUNCTION_NAME or node.id in function_aliases

        chain = _attribute_chain(node)
        if chain:
            if chain == FUNCTION_NAME:
                return True
            if not chain.endswith(f".{FUNCTION_NAME}"):
                return False
            prefix = chain[: -len(FUNCTION_NAME) - 1]
            if prefix in module_aliases:
                return True
            if prefix in module_aliases.values():
                return True
            if prefix in package_aliases.values():
                return True
            if prefix in BUILD_REGISTRY_MODULE_PATHS:
                return True
        return False

    def is_module_ref(node: ast.AST) -> bool:
        if isinstance(node, ast.Name):
            return node.id in module_aliases or node.id in package_aliases
        chain = _attribute_chain(node)
        if chain is None:
            return False
        if chain in BUILD_REGISTRY_MODULE_PATHS:
            return True
        if chain in module_aliases.values():
            return True
        if chain in package_aliases.values():
            return True
        # prefix of the attribute chain could be a module alias too
        head = chain.split(".")[0]
        if head in module_aliases or head in package_aliases:
            rest = chain[len(head) + 1 :]
            if rest.startswith("build_legacy_exception_registry"):
                return True
        return False

    def make_violation(node: ast.AST, message: str) -> Violation:
        return Violation(
            class_name=_enclosing_class_name(node, parents),
            function_name=_enclosing_function_name(node, parents),
            line=node.lineno if hasattr(node, "lineno") else 0,
            col=node.col_offset if hasattr(node, "col_offset") else 0,
            message=message,
        )

    violations: list[Violation] = []

    for node in ast.walk(tree):
        if not hasattr(node, "lineno"):
            continue
        class_name = _enclosing_class_name(node, parents)
        if class_name in allowed_classes:
            continue

        if isinstance(node, ast.Call):
            # getattr(builder, 'build_registry')
            getattr_target = _getattr_target(node)
            if getattr_target is not None and is_module_ref(getattr_target):
                violations.append(
                    make_violation(
                        node,
                        f"getattr(..., 'build_registry') outside {allowed_classes}",
                    )
                )
                continue

            # partial(builder.build_registry, ...)
            partial_arg = _partial_target(node)
            if partial_arg is not None and is_build_registry_ref(partial_arg):
                violations.append(
                    make_violation(
                        node,
                        f"partial(build_registry, ...) outside {allowed_classes}",
                    )
                )
                continue

        # Direct Name or Attribute reference (including as call func or argument)
        if isinstance(node, (ast.Name, ast.Attribute)):
            if hasattr(node, "ctx") and not isinstance(node.ctx, ast.Load):
                continue
            if is_build_registry_ref(node):
                msg = (
                    f"build_registry reference outside {allowed_classes}"
                    if isinstance(node, ast.Name)
                    else f"{_attribute_chain(node)} reference outside {allowed_classes}"
                )
                violations.append(make_violation(node, msg))

    # Deduplicate while keeping order.
    seen: set[Violation] = set()
    unique: list[Violation] = []
    for v in violations:
        if v not in seen:
            seen.add(v)
            unique.append(v)
    return unique
