package groovypp.text

/**
 * PrintWriter which does not synchronize
 *
 * This give huge (up to x10) performance gain
 */
@Typed class FastPrintWriter extends PrintWriter {
    private String lineSeparator;
    private boolean autoFlush = false;

    FastPrintWriter(Writer out, boolean autoFlush = false) {
        super(out, autoFlush)
        lineSeparator = (String) java.security.AccessController.doPrivileged(
                   new sun.security.action.GetPropertyAction("line.separator"));
        this.autoFlush = autoFlush
    }

    FastPrintWriter(OutputStream out, boolean autoFlush = false) {
        super(out, autoFlush)
        lineSeparator = (String) java.security.AccessController.doPrivileged(
                   new sun.security.action.GetPropertyAction("line.separator"));
        this.autoFlush = autoFlush
    }

    FastPrintWriter(String fileName) {
        super(fileName)
        lineSeparator = (String) java.security.AccessController.doPrivileged(
                   new sun.security.action.GetPropertyAction("line.separator"));
    }

    FastPrintWriter(String fileName, String csn) {
        super(fileName, csn)
        lineSeparator = (String) java.security.AccessController.doPrivileged(
                   new sun.security.action.GetPropertyAction("line.separator"));
    }

    FastPrintWriter(File file) {
        super(file)
        lineSeparator = (String) java.security.AccessController.doPrivileged(
                   new sun.security.action.GetPropertyAction("line.separator"));
    }

    FastPrintWriter(File file, String csn) {
        super(file, csn)
        lineSeparator = (String) java.security.AccessController.doPrivileged(
                   new sun.security.action.GetPropertyAction("line.separator"));
    }

    void flush() {
        out.flush()
    }

    void close() {
        out.close()
        out = null
    }

    void write(int c) {
        out.write(c)
    }

    void write(char[] buf, int off, int len) {
        out.write(buf, off, len)
    }

    void write(char[] buf) {
        out.write(buf)
    }

    void write(String s, int off, int len) {
        out.write(s, off, len)
    }

    void write(String s) {
        out.write(s)
    }

    void print(boolean b) {
        out.write(b ? "true" : "false")
    }

    void print(char c) {
        out.write((int)c)
    }

    void print(int i) {
        out.write(String.valueOf(i))
    }

    void print(long l) {
        out.write(String.valueOf(l))
    }

    void print(float f) {
        out.write(String.valueOf(f))
    }

    void print(double d) {
        out.write(String.valueOf(d))
    }

    void print(char[] s) {
        out.write(s, 0, s.length)
    }

    void print(String s) {
        if (s == null) {
            s = "null"
        }
        write(s)
    }

    void print(Object obj) {
        out.write(String.valueOf(obj))
    }

    void println() {
        out.write(lineSeparator);
        if (autoFlush) {
            out.flush();
        }
    }

    void println(boolean x) {
        print(x)
        println()
    }

    void println(char x) {
        print(x)
        println()
    }

    void println(int x) {
        print(x)
        println()
    }

    void println(long x) {
        print(x)
        println()
    }

    void println(float x) {
        print(x)
        println()
    }

    void println(double x) {
        print(x)
        println()
    }

    void println(char[] x) {
        print(x)
        println()
    }

    void println(String x) {
        print(x)
        println()
    }

    void println(Object x) {
        print(x)
        println()
    }

    PrintWriter printf(String format, Object[] args) {
        return super.printf(format, args)
    }

    PrintWriter printf(Locale l, String format, Object... args) {
        return super.printf(l, format, args)
    }

    PrintWriter format(String format, Object... args) {
        return super.format(format, args)
    }

    PrintWriter format(Locale l, String format, Object[] args) {
        return super.format(l, format, args)
    }

    PrintWriter append(CharSequence csq) {
        return super.append(csq)
    }

    PrintWriter append(CharSequence csq, int start, int end) {
        return super.append(csq, start, end)
    }

    PrintWriter append(char c) {
        return super.append(c)
    }
}
