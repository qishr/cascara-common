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


package io.github.qishr.cascara.common.semver;

import java.util.Objects;

// TODO doc
public class Range {
    protected final SemVer version;
    protected final RangeOperator op;

    public Range(SemVer version, RangeOperator op) {
        this.version = version;
        this.op = op;
    }

    public Range(String version, RangeOperator op) {
        this(new SemVer(version, SemVer.SemVerType.LOOSE), op);
    }

    public boolean isSatisfiedBy(String version) {
        return this.isSatisfiedBy(new SemVer(version, this.version.getType()));
    }

    public boolean isSatisfiedBy(SemVer version) {
        switch (this.op) {
            case EQ:
                return version.isEquivalentTo(this.version);
            case LT:
                return version.isLowerThan(this.version);
            case LTE:
                return version.isLowerThan(this.version) || version.isEquivalentTo(this.version);
            case GT:
                return version.isGreaterThan(this.version);
            case GTE:
                return version.isGreaterThan(this.version) || version.isEquivalentTo(this.version);
        }

        throw new RuntimeException("Code error. Unknown RangeOperator: " + this.op); // Should never happen
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Range)) return false;
        Range range = (Range) o;
        return Objects.equals(version, range.version) &&
                op == range.op;
    }

    @Override public int hashCode() {
        return Objects.hash(version, op);
    }

    @Override public String toString() {
        return this.op.asString() + this.version;
    }

    public enum RangeOperator {
        /**
         * The version and the requirement are equivalent
         */
        EQ("="),

        /**
         * The version is lower than the requirent
         */
        LT("<"),

        /**
         * The version is lower than or equivalent to the requirement
         */
        LTE("<="),

        /**
         * The version is greater than the requirement
         */
        GT(">"),

        /**
         * The version is greater than or equivalent to the requirement
         */
        GTE(">=");

        private final String s;

        RangeOperator(String s) {
            this.s = s;
        }

        public String asString() {
            return s;
        }
    }
}
