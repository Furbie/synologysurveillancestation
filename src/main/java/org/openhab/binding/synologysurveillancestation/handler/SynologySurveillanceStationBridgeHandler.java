/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.synologysurveillancestation.handler;

import java.math.BigDecimal;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.synologysurveillancestation.SynologySurveillanceStationBindingConstants;
import org.openhab.binding.synologysurveillancestation.internal.Config;
import org.openhab.binding.synologysurveillancestation.internal.discovery.CameraDiscoveryService;
import org.openhab.binding.synologysurveillancestation.internal.webapi.SynoWebApiHandler;
import org.openhab.binding.synologysurveillancestation.internal.webapi.WebApiException;
import org.openhab.binding.synologysurveillancestation.internal.webapi.response.InfoResponse;
import org.openhab.binding.synologysurveillancestation.internal.webapi.response.SynoApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SynologySurveillanceStationBridgeHandler} is a Bridge handler for camera Things
 *
 * @author Nils
 */
public class SynologySurveillanceStationBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(SynologySurveillanceStationBridgeHandler.class);
    private CameraDiscoveryService cameraDiscovery;

    private SynoWebApiHandler apiHandler = null;

    public SynologySurveillanceStationBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    public SynoWebApiHandler getSynoWebApiHandler() {
        return apiHandler;
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        // There is nothing to handle in the bridge handler
    }

    public void setDiscovery(CameraDiscoveryService discovery) {
        this.cameraDiscovery = discovery;
    }

    @Override
    public void initialize() {

        try {

            if (logger.isDebugEnabled()) {
                logger.debug("Initialize thing: {}::{}", getThing().getLabel(), getThing().getUID());
            }

            Config config = getConfigAs(Config.class);
            apiHandler = new SynoWebApiHandler(config);
            apiHandler.connect();

            InfoResponse infoResponse = apiHandler.getInfo();
            getThing().setProperty(SynoApiResponse.PROP_CAMERANUMBER,
                    infoResponse.getData().get(SynoApiResponse.PROP_CAMERANUMBER).getAsString());
            // TODO if needed add other infos

            updateStatus(ThingStatus.ONLINE);

            // Trigger discovery of cameras
            if (cameraDiscovery != null) {
                cameraDiscovery.discoverCameras();
            }

        } catch (WebApiException e) {
            if (e.getErrorCode() == 400) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Please add or check your credentials");
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Errorcode: " + e.getErrorCode());
            }
        }

    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        super.handleConfigurationUpdate(configurationParameters);
        Object refreshObj = configurationParameters.get(SynologySurveillanceStationBindingConstants.POLL);
        if (refreshObj != null) {
            int refresh = ((BigDecimal) refreshObj).intValue();
            for (Thing thing : getThing().getThings()) {
                try {
                    SynologySurveillanceStationHandler handler = (SynologySurveillanceStationHandler) thing
                            .getHandler();
                    if (handler.getRefresh() != refresh) {
                        handler.setRefresh(refresh);
                    }
                } catch (Exception e) {
                    logger.error("Exception while changing refresh rate");
                }
            }
        }
    }
}
