package com.carrierfraud.domain;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    ADMIN {
        @Override
        public Set<Department> visibleDepartments() {
            return EnumSet.allOf(Department.class);
        }
    },
    ANALYST {
        @Override
        public Set<Department> visibleDepartments() {
            return EnumSet.of(
                Department.MEDIATION,
                Department.FRAUD_INVESTIGATION,
                Department.INSURANCE,
                Department.CUSTOMER_SERVICE
            );
        }
    },
    COMPLIANCE {
        @Override
        public Set<Department> visibleDepartments() {
            return EnumSet.of(
                Department.LEGAL,
                Department.COMPLIANCE_REVIEW,
                Department.DEPARTMENT_MANAGER
            );
        }
    },
    CLIENT {
        @Override
        public Set<Department> visibleDepartments() {
            return EnumSet.noneOf(Department.class);
        }
    };
    public abstract Set<Department> visibleDepartments();
}
