import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.hqapi1.ErrorCode;

class MetricController extends ApiController {

    private Closure getMetricXML(m) {
        { doc -> 
            Metric(id             : m.id,
                   interval       : m.interval,
                   enabled        : m.enabled,
                   name           : m.template.name,
                   defalutOn      : m.template.defaultOn,
                   indicator      : m.template.designate,
                   collectionType : m.template.collectionType) {
            MetricTemplate(id              : m.template.id,
                           name            : m.template.name,
                           alias           : m.template.alias,
                           units           : m.template.units,
                           plugin          : m.template.plugin,
                           indicator       : m.template.designate,
                           defaultOn       : m.template.defaultOn,
                           collectionType  : m.template.collectionType,
                           defaultInterval : m.template.defaultInterval)
            }
        }
    }

    private Closure getMetricTemplateXML(t) {
        { doc -> 
            MetricTemplate(id              : t.id,
                           name            : t.name,
                           alias           : t.alias,
                           units           : t.units,
                           plugin          : t.plugin,
                           indicator       : t.designate,
                           defaultOn       : t.defaultOn,
                           collectionType  : t.collectionType,
                           defaultInterval : t.defaultInterval)
        }
    }

    def disableMetric(params) {
        def failureXml
        def metricId = params.getOne("id")?.toInteger()
        if (!metricId) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        try {
            metricHelper.disableMeasurement(metricId);
        } catch (Exception e) {
            log.error("UnexpectedError: " + e.getMessage(), e);
            failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
        }
        renderXml() {
            DisableMetricResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    def listMetrics(params) {
        def failureXml
        def metrics
        def resourceId = params.getOne("resourceId")?.toInteger()
        if (!resourceId) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        else {
            try {
                def res = resourceHelper.findById(resourceId)
                metrics = res.metrics
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e);
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
            }
        }
        renderXml() {
            ListMetricResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    for (m in metrics) {
                        out << getMetricXML(m)
                    }
                }
            }
        }
    }

    def getMetric(params) {
        def failureXml
        def metric
        def metricId = params.getOne("id")?.toInteger()
        if (!metricId) {
            log.error("Invalid Params: no metric id")
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        try {
            metric = metricHelper.findMeasurementById(metricId);
        } catch (Exception e) {
            log.error("UnexpectedError: " + e.getMessage(), e);
            failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
        }
        renderXml() {
            GetMetricResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getMetricXML(metric)
                }
            }
        }
    }

    def enableMetric(params) {
        def failureXml
        def metricId = params.getOne("id")?.toInteger()
        def interval = params.getOne("interval")?.toInteger()
        if (!metricId || !interval) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        //want to make sure users can't set the interval < 1 min
        interval = interval*60000
        try {
            metricHelper.enableMeasurement(metricId, interval);
        } catch (Exception e) {
            log.error("UnexpectedError: " + e.getMessage(), e);
            failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
        }
        renderXml() {
            EnableMetricResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    def setInterval(params) {
        def failureXml
        def metricId = params.getOne("id")?.toInteger()
        def interval = params.getOne("interval")?.toInteger()
        if (!metricId || !interval) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        //want to make sure users can't set the interval < 1 min
        interval = interval*60000
        try {
            metricHelper.updateMeasurementInterval(metricId, interval);
        } catch (Exception e) {
            log.error("UnexpectedError: " + e.getMessage(), e);
            failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
        }
        renderXml() {
            SetMetricIntervalResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    def getMetricTemplate(params) {
        def failureXml
        def metric
        def metricId = params.getOne("id")?.toInteger()
        if (!metricId) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        else {
            try {
                metric = metricHelper.findMeasurementById(metricId);
            } catch (Exception e) {
                log.error("UnexpectedError: " + e.getMessage(), e);
                failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
            }
        }
        renderXml() {
            GetMetricTemplateResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                    out << getMetricTemplateXML(metric.template)
                }
            }
        }
    }

    def setDefaultOn(params) {
        def failureXml
        def templateId = params.getOne("templateId")?.toInteger()
        def on = params.getOne("on")?.toBoolean()
        if (!templateId) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        try {
            metricHelper.setDefaultOn(templateId, on);
        } catch (Exception e) {
            log.error("UnexpectedError: " + e.getMessage(), e);
            failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
        }
        renderXml() {
            SetMetricDefaultOnResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    def setDefaultIndicator(params) {
        def failureXml
        def templateId = params.getOne("templateId")?.toInteger()
        def on = params.getOne("on")?.toBoolean()
        if (!templateId) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        try {
            metricHelper.setDefaultIndicator(templateId, on);
        } catch (Exception e) {
            log.error("UnexpectedError: " + e.getMessage(), e);
            failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
        }
        renderXml() {
            SetMetricDefaultIndicatorResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    def setDefaultInterval(params) {
        def failureXml
        def templateId = params.getOne("templateId")?.toInteger()
        def interval = params.getOne("interval")?.toInteger()
        if (!templateId || !interval) {
            failureXml = getFailureXML(ErrorCode.INVALID_PARAMETERS)
        }
        //want to make sure users can't set the interval < 1 min
        interval = interval*60000
        try {
            metricHelper.setDefaultInterval(templateId, interval);
        } catch (Exception e) {
            log.error("UnexpectedError: " + e.getMessage(), e);
            failureXml = getFailureXML(ErrorCode.UNEXPECTED_ERROR)
        }
        renderXml() {
            SetMetricDefaultIntervalResponse() {
                if (failureXml) {
                    out << failureXml
                } else {
                    out << getSuccessXML()
                }
            }
        }
    }

    def pumpData(params) {
        //TODO: fill in method
    }
}