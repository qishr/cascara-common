// # License & Terms
//
// This file is part of **Cascara**.
//
// **Cascara** is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
//
// ---
//
// ## Special Runtime Exception
//
// As a special exception, the copyright holders of this library give you
// permission to link this library with independent modules to produce an
// executable, regardless of the license terms of these independent modules,
// and to copy and distribute the resulting executable under terms of your
// choice, provided that you also meet, for each linked independent module,
// the terms and conditions of the license of that module.
//
// An independent module is a module which is not derived from or based on
// this library. If you modify this library, you may extend this exception
// to your version of the library, but you are not obligated to do so. If
// you do not wish to do so, delete this exception statement from your
// version.


package io.github.qishr.cascara.common.util;

import java.io.Serial;
import java.util.Objects;

public class Pair<L,R> {
    @Serial
    private static final long serialVersionUID = 1L;

    private L l;
    private R r;

    public Pair(L l, R r) {
        this.l = l;
        this.r = r;
    }

    public L getL() {
        return l;
    }

    public R getR() {
        return r;
    }

    public void setL(L l) {
        this.l = l;
    }

    public void setR(R r) {
        this.r = r;
    }

    public String toString() {
        return "Pair[" + l + "," + r + "]";
    }

    public boolean equals(Object other) {
        return other instanceof Pair<?,?> pair &&
            Objects.equals(l, pair.l) &&
            Objects.equals(r, pair.r);
    }

    public int hashCode() {
        if (l == null) return (r == null) ? 0 : r.hashCode() + 1;
        else if (r == null) return l.hashCode() + 2;
        else return l.hashCode() * 17 + r.hashCode();
    }

    public static <A,B> Pair<A,B> of(A a, B b) {
        return new Pair<>(a,b);
    }
}
