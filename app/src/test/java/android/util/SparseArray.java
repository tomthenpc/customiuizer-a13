package android.util;

/**
 * Functional JVM shadow of {@link android.util.SparseArray} for unit tests.
 *
 * The Android framework jar used in local unit tests only contains stubs that
 * return default values, so a real sparse map implementation is needed for tests
 * that assert on the state held by {@code ResourceHooks#active}.
 */
public class SparseArray<E> implements Cloneable {

    private int[] mKeys;
    private Object[] mValues;
    private int mSize;

    public SparseArray() {
        this(10);
    }

    public SparseArray(int initialCapacity) {
        if (initialCapacity == 0) {
            mKeys = new int[0];
            mValues = new Object[0];
        } else {
            mKeys = new int[initialCapacity];
            mValues = new Object[initialCapacity];
        }
        mSize = 0;
    }

    public E get(int key) {
        return get(key, null);
    }

    @SuppressWarnings("unchecked")
    public E get(int key, E valueIfKeyNotFound) {
        int i = binarySearch(key);
        if (i < 0 || mValues[i] == null) {
            return valueIfKeyNotFound;
        }
        return (E) mValues[i];
    }

    public void delete(int key) {
        remove(key);
    }

    public void remove(int key) {
        int i = binarySearch(key);
        if (i >= 0) {
            removeAt(i);
        }
    }

    public void removeAt(int index) {
        System.arraycopy(mKeys, index + 1, mKeys, index, mSize - (index + 1));
        System.arraycopy(mValues, index + 1, mValues, index, mSize - (index + 1));
        mSize--;
    }

    public void put(int key, E value) {
        int i = binarySearch(key);
        if (i >= 0) {
            mValues[i] = value;
        } else {
            i = ~i;
            if (mSize >= mKeys.length) {
                growAndInsert(key, value, i);
            } else {
                if (mSize - i != 0) {
                    System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i);
                    System.arraycopy(mValues, i, mValues, i + 1, mSize - i);
                }
                mKeys[i] = key;
                mValues[i] = value;
                mSize++;
            }
        }
    }

    public int size() {
        return mSize;
    }

    public int keyAt(int index) {
        return mKeys[index];
    }

    @SuppressWarnings("unchecked")
    public E valueAt(int index) {
        return (E) mValues[index];
    }

    public void setValueAt(int index, E value) {
        mValues[index] = value;
    }

    public int indexOfKey(int key) {
        return binarySearch(key);
    }

    public int indexOfValue(E value) {
        for (int i = 0; i < mSize; i++) {
            if (mValues[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public void clear() {
        mSize = 0;
    }

    public void append(int key, E value) {
        put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SparseArray<E> clone() {
        SparseArray<E> clone;
        try {
            clone = (SparseArray<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        clone.mKeys = mKeys.clone();
        clone.mValues = mValues.clone();
        clone.mSize = mSize;
        return clone;
    }

    @Override
    public String toString() {
        if (mSize <= 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder(mSize * 28);
        sb.append('{');
        for (int i = 0; i < mSize; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(keyAt(i)).append("=").append(valueAt(i));
        }
        sb.append('}');
        return sb.toString();
    }

    private void growAndInsert(int key, E value, int insertion) {
        int n = mSize;
        int newSize = Math.max(n + 1, mKeys.length * 2);
        int[] newKeys = new int[newSize];
        Object[] newValues = new Object[newSize];

        System.arraycopy(mKeys, 0, newKeys, 0, insertion);
        System.arraycopy(mValues, 0, newValues, 0, insertion);
        newKeys[insertion] = key;
        newValues[insertion] = value;

        System.arraycopy(mKeys, insertion, newKeys, insertion + 1, n - insertion);
        System.arraycopy(mValues, insertion, newValues, insertion + 1, n - insertion);

        mKeys = newKeys;
        mValues = newValues;
        mSize = n + 1;
    }

    private int binarySearch(int key) {
        int lo = 0;
        int hi = mSize - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midVal = mKeys[mid];
            if (midVal < key) {
                lo = mid + 1;
            } else if (midVal > key) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return ~lo;
    }
}
