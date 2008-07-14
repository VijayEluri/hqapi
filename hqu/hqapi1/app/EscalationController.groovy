import org.hyperic.hq.hqu.rendit.BaseController

import org.hyperic.hq.authz.shared.PermissionException
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig
import org.hyperic.hq.events.NoOpAction
import org.hyperic.hq.hqapi1.ErrorCode

class EscalationController extends ApiController {
    private Closure getEscalationXML(e) {
        { doc -> 
            Escalation(id :           e.id,
                       name :         e.name,
                       description :  e.description,
                       pauseAllowed : e.pauseAllowed,
                       maxPauseTime : e.maxPauseTime,
                       notifyAll :    e.notifyAll,
                       repeat :       e.repeat) {
                for (ea in e.actions) {
                    def a = ea.action
                    Action(id : a.id,
                           wait : ea.waitTime,
                           actionType : (a.className  =~ /.+\.([A-Za-z]+)/) [0][1]
                    )
                }
            }
        }
    }

    def get(params) {
        def id = params.getOne("id")?.toInteger()
        def name = params.getOne("name")
        
        def esc = escalationHelper.getEscalation(id, name)
        renderXml() {
            out << GetEscalationResponse() {
                if (!esc) {
                    out << getFailureXML(ErrorCode.OBJECT_NOT_FOUND)
                }
                else {
                    out << getSuccessXML()
                    out << getEscalationXML(esc)
                }
            }
        }
    }

    def list(params) {
        renderXml() {
            out << ListEscalationsResponse() {
                out << getSuccessXML()
                for (e in escalationHelper.allEscalations.sort {a, b -> a.name <=> b.name}) {
                    out << getEscalationXML(e)
                }
            }
        }
    }

    def delete(params) {
        def id = params.getOne("id").toInteger()
        def esc = escalationHelper.deleteEscalation(id)
        renderXml() {
            out << DeleteEscalationResponse() {
                out << getSuccessXML()
            }
        }
    }
    
    def create(params) {
        def syncRequest = new XmlParser().parseText(getUpload('postdata'))

        for (xmlEsc in syncRequest['Escalation']) {
            def name         = xmlEsc.'@name'
            def desc         = xmlEsc.'@description'
            def pauseAllowed = xmlEsc.'@pauseAllowed'.toBoolean()
            def maxWaitTime  = xmlEsc.'@maxPauseTime'.toLong()
            def notifyAll    = xmlEsc.'@notifyAll'.toBoolean()
            def repeat       = xmlEsc.'@repeat'.toBoolean()

            def esc = escalationHelper.createEscalation(name, desc,
                                                        pauseAllowed,
                                                        maxWaitTime, notifyAll,
                                                        repeat)
            syncActions(esc, xmlEsc['Action'])

            renderXml() {
                out << CreateEscalationResponse() {
                    out << getSuccessXML()
                    out << getEscalationXML(esc)
                }
            }
        }
    }
    
    def update(params) {
        def syncRequest = new XmlParser().parseText(getUpload('postdata'))
        
        for (xmlEsc in syncRequest['Escalation']) {
            def id 			 = xmlEsc.'@id'?.toInteger()
            def name         = xmlEsc.'@name'
            def desc         = xmlEsc.'@description'
            def pauseAllowed = xmlEsc.'@pauseAllowed'.toBoolean()
            def maxWaitTime  = xmlEsc.'@maxPauseTime'.toLong()
            def notifyAll    = xmlEsc.'@notifyAll'.toBoolean()
            def repeat       = xmlEsc.'@repeat'.toBoolean()
        
            def esc = escalationHelper.getEscalation(id, name)
            escalationHelper.updateEscalation(esc, name, desc, pauseAllowed,
                                              maxWaitTime, notifyAll, repeat)
            
            syncActions(esc, xmlEsc['Action'])
            
            renderXml() {
                out << UpdateEscalationResponse() {
                    out << getSuccessXML()
                    out << getEscalationXML(esc)
                }
            }
        }
    }
    
    def syncActions(esc, actions) {
        // Remove all actions
        while (esc.actions.size() > 0) {
            escMan.removeAction(esc, esc.actions.get(0).action.id)
        }

        for (xmlAct in actions) {
            def action = null

            switch (xmlAct.'@actionType') {
            case 'EmailAction' :
                // Create Email action
                action =
                    Class.forName(EmailActionConfig.implementor).newInstance()
                action.setSms(xmlAct.'@sms'.toBoolean())

                switch (xmlAct.'@notifyType') {
                case 'notifyRoles' :
                    action.setType(EmailActionConfig.TYPE_ROLES)
                    break
                case 'notifyUsers' :
                    action.setType(EmailActionConfig.TYPE_USERS)
                    break
                default :
                    action.setType(EmailActionConfig.TYPE_EMAILS)
                    break
                }

                // Get all of the names to notify
                def names = xmlAct['Notify'].collect { notifyDef ->
                    def name = notifyDef.'@name'
                    switch (action.getType()) {
                        case EmailAction.TYPE_ROLES:
                            name = roleHelper.findRoleByName(name).id
                            break;
                        case EmailAction.TYPE_USERS:
                            name = userHelper.findUser(name).id
                            break;
                        case EmailAction.TYPE_EMAILS:
                        default:
                            break;
                    }
                    name
                }

                action.setNames(names.join(","))
                break
            case 'NoOpAction' :
                action = new NoOpAction()
                break
            }

            if (action != null)
                escalationHelper.addAction(esc, action, xmlAct.'@wait'.toLong())
        }
    }
}