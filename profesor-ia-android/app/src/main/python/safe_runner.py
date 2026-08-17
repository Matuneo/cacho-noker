import contextlib
import io
import json
import sys
import time
import traceback


SAFE_BUILTINS = {
    "abs": abs, "all": all, "any": any, "bool": bool, "dict": dict,
    "enumerate": enumerate, "filter": filter, "float": float,
    "format": format, "frozenset": frozenset, "int": int, "isinstance": isinstance,
    "len": len, "list": list, "map": map, "max": max, "min": min,
    "next": next, "object": object, "pow": pow, "print": print,
    "range": range, "repr": repr, "reversed": reversed, "round": round,
    "set": set, "slice": slice, "sorted": sorted, "str": str, "sum": sum,
    "tuple": tuple, "type": type, "zip": zip,
    "Exception": Exception, "ValueError": ValueError, "TypeError": TypeError,
    "KeyError": KeyError, "IndexError": IndexError, "ZeroDivisionError": ZeroDivisionError,
}


class ExecutionLimit(Exception):
    pass


def run_code(code, timeout_seconds=3):
    if len(code) > 20000:
        return json.dumps({"success": False, "output": "", "error": "El código supera el límite de 20.000 caracteres.", "line": 0}, ensure_ascii=False)

    output = io.StringIO()
    errors = io.StringIO()
    started = time.monotonic()

    def watchdog(frame, event, arg):
        if time.monotonic() - started > float(timeout_seconds):
            raise ExecutionLimit("La ejecución superó el límite de tiempo de 3 segundos.")
        return watchdog

    try:
        compiled = compile(code, "laboratorio.py", "exec")
        scope = {"__builtins__": SAFE_BUILTINS, "__name__": "__main__"}
        sys.settrace(watchdog)
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(errors):
            exec(compiled, scope, scope)
        return json.dumps({"success": True, "output": output.getvalue() or "Programa finalizado sin salida.", "error": errors.getvalue(), "line": 0}, ensure_ascii=False)
    except BaseException as exc:
        line = 0
        tb = traceback.extract_tb(exc.__traceback__)
        for frame in reversed(tb):
            if frame.filename == "laboratorio.py":
                line = frame.lineno
                break
        return json.dumps({"success": False, "output": output.getvalue(), "error": f"{type(exc).__name__}: {exc}", "line": line}, ensure_ascii=False)
    finally:
        sys.settrace(None)

