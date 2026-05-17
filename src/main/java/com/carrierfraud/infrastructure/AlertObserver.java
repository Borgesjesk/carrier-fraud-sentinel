package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.RiskAlert;

public interface AlertObserver {

    void notify(RiskAlert alert);
}