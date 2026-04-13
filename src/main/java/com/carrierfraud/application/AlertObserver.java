package com.carrierfraud.application;

import com.carrierfraud.domain.RiskAlert;

public interface AlertObserver {

    void notify(RiskAlert alert);
}
