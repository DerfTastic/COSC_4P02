const apiRoot = "/api";


class UserPayment{
    /** @type{string} */name
    /** @type{string} */billing
    /** @type{string} */card
    /** @type{string} */exparation
    /** @type{string} */code
}

class TicketOrderItem{
    type = "Ticket"
    /** @type{number} */id

    constructor(id){
        this.id = id;
    }
}

class AccountOrganizerUpgradeOrderItem{
    type = "AccountOrganizerUpgrade"
}

/** @typedef{(TicketOrderItem|AccountOrganizerUpgradeOrderItem)} UserOrderItem */

class UserOrder{
    /** @type{UserPayment} */ payment
    /** @type{UserOrderItem[]} */ items
}

class Receipt{
    /** @type{number} */payment_id
    /** @type{ReceiptItem[]} */items
    /** @type{number} */date
    /** @type{number} */subtotal
    /** @type{number} */fees
    /** @type{number} */gst
    /** @type{number} */total
}

class ScanResult{
    /** @type{UserInfo} */userinfo
    /** @type{OurEvent} */event
    /** @type{EventTicket} */ticket
    /** @type{boolean} */purchase_matches
    /** @type{Scanned}} */scans
}

class Scanned{
    /** @type{number} */date
}

class QRCodeScan{
    /** @type{number} */event_id
    /** @type{string} */name
    /** @type{PurchasedTicketId} */id
}

class PurchasedTicketId{
    /** @type{number} */ id
    /** @type{string} */ salt
}

class ReceiptItem{
    /** @type{"Ticket"|"AccountOrganizerUpgrade"} */type
    /** @type{string?} */name
    /** @type{PurchasedTicketId?} */id
    /** @type{number?} */purchase_price
}

class BillEstimate{
    /** @type{ReceiptItem[]} */items
    /** @type{number} */subtotal
    /** @type{number} */fees
    /** @type{number} */gst
    /** @type{number} */total
}

class UserInfo {
    /** @type{number} */id
    /** @type{string} */name
    /** @type{string} */email
    /** @type{string?} */bio
    /** @type{string?} */disp_email
    /** @type{string?} */disp_phone_number
    /** @type{boolean} */organizer
    /** @type{boolean} */admin
    /** @type{number} */picture
    /** @type{number} */banner
}

class UpdateUserInfo{
    /** @type{string?} */name
    /** @type{string?} */bio
    /** @type{string?} */disp_email
    /** @type{string?} */disp_phone_number
}

class ChangeUserAuth{
    /** @type{string} */old_email
    /** @type{string} */old_password
    /** @type{string?} */new_email
    /** @type{string?} */new_password
}

class Log {
    /** @type{string} */level_s
    /** @type{number} */level_i
    /** @type{string} */message
    /** @type{number} */millis
    /** @type{number} */sequenceNumber
    /** @type{string} */sourceClassName
    /** @type{string} */sourceMethodName
    /** @type{string} */thrown
}

class EventTicket {
    /** @type{number} */id
    /** @type{string} */name
    /** @type{number} */price
    /** @type{number} */available_tickets
}

class EventTicketUpdate {
    /** @type{string?} */name
    /** @type{number?} */price
    /** @type{number?} */available_tickets
}

class OrganizerEvent {
    /** @type{number} */id
    /** @type{number} */owner_id
    /** @type{string} */name
    /** @type{string} */description
    /** @type{string} */type
    /** @type{string} */category
    /** @type{number} */picture
    /** @type{object} */metadata
    /** @type{boolean} */draft

    /** @type{number?} */start
    /** @type{number?} */duration
    /** @type{number} */release_time

    /** @type{string} */location_name
    /** @type{number} */location_lat
    /** @type{number} */location_long

    /** @type{string[]} */tags

    /** @type{UserInfo?} */owner
}

class UpdateOrganizerEvent{
    /** @type{string?} */name
    /** @type{string?} */description
    /** @type{object?} */metadata

    /** @type{number?} */start
    /** @type{number?} */duration

    /** @type{string?} */type
    /** @type{string?} */category
    /** @type{number?} */release_time

    /** @type{string?} */location_name
    /** @type{number?} */location_lat
    /** @type{number?} */location_long
}

class RouteStat{
    /** @type{number} */total_response_time_ns
    /** @type{number} */requests_handled
    /** @type{Object.<number, number>} */code_breakdown
}

class DbStat{
    /** @type{number} */rw_statements_executed
    /** @type{number} */ro_statements_executed
    
    /** @type{number} */rw_prepared_statements_executed
    /** @type{number} */ro_prepared_statements_executed

    /** @type{number} */rw_db_acquires
    /** @type{number} */ro_db_acquires

    /** @type{number} */rw_db_releases
    /** @type{number} */ro_db_releases

    /** @type{number} */rw_db_lock_waited
    /** @type{number} */ro_db_lock_waited

    /** @type{number} */rw_db_lock_waited_ns
    /** @type{number} */ro_db_lock_waited_ns

    /** @type{number} */rw_db_lock_held_ns
    /** @type{number} */ro_db_lock_held_ns
}

class DbStats{
    /** @type{Object.<string, DbStat>} */individual
    /** @type{DbStat} */global
}

class ServerStatistics{
    /** @type{Object.<string, RouteStat>} */route_stats
    /** @type{number} */total_requests_handled

    /** @type{DbStats} */db_stats

    /** @type{number} */curr_time_ms
    /** @type{number} */max_mem
    /** @type{number} */total_mem
    /** @type{number} */free_mem
}

class Search{
    /** @type{number?} */ date_start
    /** @type{number?} */ date_end
    /** @type{number?} */ max_duration
    /** @type{number?} */ min_duration
    /** @type{OrganizerEventTag[]?} */ tags
    /** @type{string} */type_fuzzy
    /** @type{string} */category_fuzzy
    /** @type{string?} */ organizer_fuzzy
    /** @type{string?} */ name_fuzzy
    /** @type{number?} */ organizer_exact
    /** @type{number?} */ distance
    /** @type{number?} */ location_lat
    /** @type{number?} */ location_long
    /** @type{boolean?} */ owning
    /** @type{boolean?} */ draft
    /** @type{string?} */ location
    /** @type{number?} */ offset
    /** @type{number?} */ limit
    /** @type{"MinPrice"|"MaxPrice"|"TicketsAvailable"|"Closest"|"StartTime"|"MinDuration"|"MaxDuration"|"Nothing"} */ sort_by
}

// actually just a string but shhhh
/** @type{string} */
class Session { }

const api = {
    /**
     * @param {string} route 
     * @param {RequestInit} data 
     * @param {string} error 
     * @returns {Promise<Response>}
     */
    api_call: async function (route, data, error) {
        var response;
        try {
            response = await fetch(`${apiRoot}${route}`, data);
        } catch (e) {
            console.log(e);
            throw { error, code: -1 };
        }

        if (response.ok) {
            return response;
        } else if (response.status >= 400 && response.status < 500) {
            throw { error: await response.text(), code: response.status };
        } else {
            throw { error, code: response.status };
        }
    },

    payment: {
        /**
         * @param {UserOrder} order 
         * @param {Session} session 
         * @returns {Promise<ReceiptItem>}
         */
        make_purchase: async function(order, session = cookies.getSession()){
            return await (await api.api_call(
                `/make_purchase`,
                {
                    method: 'POST',
                    headers: {
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(order)
                },
                "An error occured when processing the transaction"
            )).json();
        },

        /**
         * @param {UserOrder} order 
         * @param {Session} session 
         * @returns {Promise<BillEstimate>}
         */
        create_estimate: async function(order, session = cookies.getSession()){
            return await (await api.api_call(
                `/create_estimate`,
                {
                    method: 'POST',
                    headers: {
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(order)
                },
                "An error occured when processing the transaction"
            )).json();
        },

        /**
         * @param {Session} session 
         * @returns {Promise<Receipt>}
         */
        list_receipts: async function(session = cookies.getSession()){
            return await (await api.api_call(
                `/list_receipts`,
                {
                    method: 'POST',
                    headers: {
                        'X-UserAPIToken': session
                    },
                },
                "An error occured when fetching the receipts"
            )).json();
        },
    },

    tickets: {
        /**
         * @param {number} ticket_id 
         * @param {Session} session 
         * @returns {Promise<integer>}
         */
        create_ticket: async function(event_id, session = cookies.getSession()){
            return await (await api.api_call(
                `/create_ticket/${event_id}`,
                {
                    method: 'POST',
                    headers: {
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while creating ticket"
            )).json();
        },
        /**
         * @param {number} id
         * @param {EventTicketUpdate} ticket 
         * @param {Session} session 
         * @returns {Promise}
         */
        update_ticket: async function(id, ticket, session = cookies.getSession()){
            await api.api_call(
                `/update_ticket/${id}`,
                {
                    method: 'POST',
                    headers: {
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(ticket),
                },
                "An error occured while updating ticket"
            )
        },
        /**
         * @param {number} ticket_id 
         * @param {Session} session 
         * @returns {Promise}
         */
        delete_ticket: async function(ticket_id, session = cookies.getSession()){
            await api.api_call(
                `/delete_ticket/${ticket_id}`,
                {
                    method: 'DELETE',
                    headers: {
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while deleting ticket"
            )
        },
        /**
         * @param {number} event_id 
         * @param {Session} session 
         * @returns {Promise<EventTicket[]>}
         */
        get_tickets: async function(event_id, session = cookies.getSession()){
            return await (await api.api_call(
                `/get_tickets/${event_id}`,
                {
                    method: 'GET',
                    headers: {
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting tickets"
            )).json();
        },
    },

    admin: {
        /**
         * @param {string} sql 
         * @param {Session} session 
         * @returns {Promise<string>}
         */
        execute_sql: async function (sql, session = cookies.getSession()) {
            return await (await api.api_call(
                `/execute_sql`,
                {
                    method: 'POST',
                    headers: {
                        'X-UserAPIToken': session
                    },
                    body: sql,
                },
                "An error occured while executing sql"
            )).text();
        },

        /**
         * @param {Session} session 
         * @returns {Promise<Log[]>}
         */
        get_server_logs: async function (session = cookies.getSession()) {
            return await (await api.api_call(
                `/get_server_logs`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting the server logs"
            )).json();
        },
        
        /**
         * @param {Session} session 
         * @returns {Promise<ServerStatistics>}
         */
        get_server_statistics: async function(session = cookies.getSession()) {
            return await (await api.api_call(
                `/get_server_statistics`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting the route statistics"
            )).json();
        },
        
        /**
         * @param {string} email 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        delete_other_account: async function(email, session = cookies.getSession()) {
            await api.api_call(
                `/delete_other_account/${encodeURI(email)}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while deleting that account"
            )
        },

        /**
         * @param {boolean} admin 
         * @param {string} email 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        set_account_admin: async function(admin, email, session = cookies.getSession()) {
            await api.api_call(
                `/set_account_admin/${encodeURI(admin)}/${encodeURI(email)}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while setting that account as admin"
            )
        },

        /**
         * @param {string} level 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        set_log_level: async function(level, session = cookies.getSession()) {
            await api.api_call(
                `/set_log_level/${encodeURI(level)}`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while setting the log level"
            )
        },

        /**
         * @param {Session} session 
         * @returns {Promise<string>}
         */
        get_log_level: async function(session = cookies.getSession()) {
            return await (await api.api_call(
                `/get_log_level`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting the log level"
            )).text();
        },

        /**
         * @param {Session} session 
         * @returns {Promise<string[]>}
         */
        get_log_levels: async function(session = cookies.getSession()) {
            return await (await api.api_call(
                `/get_log_levels`,
                {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                },
                "An error occured while getting the log levels"
            )).json();
        }
    },

    events: {
        /**
         * @param {Session} session 
         * @returns {Promise<number>}
         */
        create_event: async function (session = cookies.getSession()) {
            return await (await api.api_call(
                '/create_event',
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while creating event"
            )).json();
        },

        /**
         * @param {number|string} id 
         * @param {Session?} session 
         * @returns {Promise<OrganizerEvent>}
         */
        get_event: async function(id, session = cookies.getSession()){
            return await (await api.api_call(
                `/get_event/${encodeURI(id)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while getting event"
            )).json();
        },

        /**
         * @param {number|string} id 
         * @param {Session?} session 
         * @returns {Promise<OrganizerEvent>}
         */
        get_event_with_owner: async function(id, session = cookies.getSession()){
            return await (await api.api_call(
                `/get_event/${encodeURI(id)}?with_owner`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while getting event"
            )).json();
        },

        /**
         * @param {number} id
         * @param {UpdateOrganizerEvent} update 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        update_event: async function(id, update, session = cookies.getSession()){
            await api.api_call(
                `/update_event/${encodeURI(id)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(update),
                },
                "An error occured while updating event"
            );
        },

        /**
         * @param {number|string} id 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        delete_event: async function(id, session = cookies.getSession()){
            await api.api_call(
                `/delete_event/${encodeURI(id)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while deleting event"
            );
        },

        /**
         * @param {number|string} id 
         * @param {string} tag 
         * @param {boolean} category 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        add_event_tag: async function(id, tag, session = cookies.getSession()){
            await api.api_call(
                `/add_event_tag/${encodeURI(id)}/${encodeURI(tag)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while adding event tag"
            );
        },

        /**
         * @param {number|string} id 
         * @param {string} tag 
         * @param {boolean} category 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        delete_event_tag: async function(id, tag, session = cookies.getSession()){
            await api.api_call(
                `/delete_event_tag/${encodeURI(id)}/${encodeURI(tag)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while deleting event tag"
            );
        },

        /**
         * @param {number|string} id 
         * @param {boolean} draft 
         * @param {Session} session 
         * @returns {Promise<>}
         */
        set_draft: async function(id, draft, session = cookies.getSession()){
            await api.api_call(
                `/set_draft/${encodeURI(id)}/${encodeURI(draft)}`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while setting event draft"
            );
        },

        /**
         * @param {number|string} id 
         * @param {Blob} data 
         * @param {Session} session 
         * @returns {Promise<number>}
         */
        set_picture: async function(id, data, session = cookies.getSession()){
            return await (await api.api_call(
                `/set_event_picture/${encodeURI(id)}/`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: data,
                },
                "An error occured while setting event picture"
            )).json();
        }
    },


    search: {
        /**
         * @param {Search} search 
         * @param {Session} session 
         * @returns {Promise<OrganizerEvent[]>}
         */
        search_events: async function(search, session = cookies.getSession()){
            return await (await api.api_call(
                '/search_events',
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(search),
                },
                "An error occured while searching"
            )).json();
        },

        /**
         * @param {Search} search 
         * @param {Session} session 
         * @returns {Promise<OrganizerEvent[]>}
         */
        search_events_with_owner: async function(search, session = cookies.getSession()){
            return await (await api.api_call(
                '/search_events?with_owner',
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(search),
                },
                "An error occured while searching"
            )).json();
        }
    },

    organizer: {
        
        /**
         * @param {QRCodeScan} scan 
         * @param {Session} session 
         * @returns {Promise<ScanResult>}
         */
        scan_ticket: async function(scan, session = cookies.getSession()){
            return await (await api.api_call(
                '/scan_ticket',
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(scan),
                },
                "An error occured while scanning ticket"
            )).json();
        },

        /**
         * @param {Search} search 
         * @returns {Promise<OrganizerEvent[]>}
         */
        get_drafts: async function (session = cookies.getSession()) {
            return await api.search.search_events({draft: true}, session = cookies.getSession())
        },

        /**
         * @param {Search} search 
         * @returns {Promise<OrganizerEvent[]>}
         */
        get_events: async function (session = cookies.getSession()) {
            return await api.search.search_events({draft: false, owning: true}, session = cookies.getSession())
        }
    },

    user: {
        /**
         * @param {number} id
         * @param {Session} session 
         * @returns {Promise<UserInfo>}
         */
        userinfo: async function (id, session = cookies.getSession()) {
            if(id==undefined||id==null)
                id=""
            const result = await (await api.api_call(
                `/userinfo/${id}`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    }
                },
                "An error occured while fetching userinfo"
            )).json();
            return result;
        },
        /**
         * @param {UpdateUserInfo} update
         * @param {Session} session 
         * @returns {Promise<UserInfo>}
         */
        update_user: async function (update, session = cookies.getSession()) {
            await api.api_call(
                `/update_user`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(update)
                },
                "An error occured while updating user info"
            )
        },

        /**
         * @param {Blob} data 
         * @param {Session} session 
         * @returns {Promise<number>}
         */
        set_user_picture: async function(data, session = cookies.getSession()){
            return await (await api.api_call(
                `/set_user_picture`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: data,
                },
                "An error occured while setting profile picture"
            )).json();
        },
        /**
         * @param {Blob} data 
         * @param {Session} session 
         * @returns {Promise<number>}
         */
        set_user_banner_picture: async function(data, session = cookies.getSession()){
            return await (await api.api_call(
                `/set_user_banner_picture`,
                {
                    method: "POST",
                    headers: {
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: data,
                },
                "An error occured while setting profile picture"
            )).json();
        },

        /**
         * @param {ChangeUserAuth} update
         * @param {Session} session 
         * @returns {Promise<>}
         */
        change_auth: async function (update, session = cookies.getSession()) {
            await api.api_call(
                `/change_auth`,
                {
                    method: 'POST',
                    headers: { 
                        'Content-Type': 'application/json',
                        'X-UserAPIToken': session
                    },
                    body: JSON.stringify(update)
                },
                "An error occured while updating the user auth"
            );
        },

        /**
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise<Session>}
         */
        login: async function (email, password) {
            return await (await api.api_call(
                `/login`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        email: email,
                        password: password
                    })
                },
                "An error occured while loggin in"
            )).text();
        },

        /**
         * @param {string} name 
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise}
         */
        register: async function (name, email, password) {
            await api.api_call(
                `/register`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, email, password })
                },
                "An error occured while registering"
            );
        },

        /**
         * @param {Session} session 
         * @returns {Promise<Session[]>}
         */
        list_sessions: async function (session = cookies.getSession()) {
            return await (await api.api_call(
                `/list_sessions`,
                {
                    method: 'GET',
                    headers: { 'X-UserAPIToken': session }
                },
                "An error occured while fetching sessions"
            )).json();
        },

        /**
         * @param {number|string} sessionId 
         * @param {Session} session 
         * @returns {Promise}
         */
        invalidate_session: async function (sessionId, session = cookies.getSession()) {
            await api.api_call(
                `/invalidate_session/${sessionId}`,
                {
                    method: 'DELETE',
                    headers: { 'X-UserAPIToken': session }
                },
                "An error occured while invalidating session"
            );
        },

        /**
         * @param {string} email 
         * @param {string} password 
         * @param {Session} session 
         * @returns {Promise}
         */
        delete_account: async function (email, password, session = cookies.getSession()) {
            await api.api_call(
                `/delete_account`,
                {
                    method: 'DELETE',
                    headers: { 'X-UserAPIToken': session },
                    body: JSON.stringify({ email, password })
                },
                "An error occured while invalidating session"
            );
        },
        /**
         * Sends a password reset email to this email
         * @param {string} email 
         * @returns {Promise}
         */
        reset_password: async function (email) {
            await api.api_call(
                `/reset_password`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: email
                },
                "An error occured while sending reset password email"
            );
        },
        /**
         * Actually reset the password
         * @param {string} email 
         * @param {string} password 
         * @param {string} token 
         * @returns {Promise}
         */
        do_reset_password: async function (email, password, token) {
            await api.api_call(
                `/do_reset_password`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password, token })
                },
                "An error occured while resetting password"
            );
        },
    },
}


const cookies = {
    secure_flag: function (){
        if (location.protocol === 'https:') {
            return "Secure";
        }else{
            return "";
        }
    }(),
    /**
     * 
     * @param {string} token 
     * @param {number} days
     */
    setSession: function (token, days = 30) {
        const expires = new Date();
        expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
        document.cookie = `sessionToken=${encodeURIComponent(token)};expires=${expires.toUTCString()};path=/;${cookies.secure_flag}`;
    },

    /**
     * @returns {Session}
     */
    getSession: function () {
        const cookies = document.cookie.split('; ');
        for (const cookie of cookies) {
            const [key, value] = cookie.split('=');
            if (key === 'sessionToken') {
                return decodeURIComponent(value);
            }
        }
        return "";
    },

    deleteSessionToken: function () {
        document.cookie = `sessionToken=;expires=Thu, 01 Jan 1970 00:00:00 UTC;path=/;${cookies.secure_flag}`;
    },
}

const utility = {
    logout: function () {
        cookies.deleteSessionToken();
        window.location.href = '/account/login';
    },
    /**
     * @param {number|string} id 
     * @returns {boolean}
     */
    is_session_id_current: function (id) {
        if (cookies.getSession() == null || cookies.getSession().length == 0) return false;
        const curr_id = cookies.getSession().substring(cookies.getSession().length - 8, cookies.getSession().length);
        return parseInt(curr_id, 16) == id;
    },
    /**
     * @param {string} session 
     * @returns {number|null}
     */
    get_id_from_session: function (session = cookies.getSession()) {
        if (session == null || session.length == 0) return null;
        const curr_id = session.substring(session.length - 8, session.length);
        return parseInt(curr_id, 16);
    },
    is_logged_in: function() {
        return cookies.getSession() != null && cookies.getSession().length > 0;
    },
    require_logged_in: function () {
        if (!this.is_logged_in()) {
            window.location.href = "/account/login";
        }
    }

}


const page = {

    login: {
        /**
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise}
         */
        login: async function (email, password) {
            try {
                cookies.deleteSessionToken();
                cookies.setSession(await api.user.login(email, password));
                window.location.href = '/account';
            } catch ({ error, code }) {
                alert(error);
            }
        },
    },

    register: {
        /**
         * @param {string} name 
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise}
         */
        register: async function (name, email, password) {
            try {
                await api.user.register(name, email, password);
                window.location.href = '/account/login';
            } catch ({ error, code }) {
                alert(error);
            }
        },
    },

    account: {
        /**
         * @returns {Promise<UserInfo>}
         */
        userinfo: async function () {
            try {
                return await api.user.userinfo(null, cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },

        /**
         * @param {Element}
         * @param {number|string} id 
         * @returns {Promise}
         */
        remove_session: async function (element, id) {
            try {
                await api.user.invalidate_session(id, cookies.getSession());
                element.parentElement.outerHTML = "";
                if (utility.is_session_id_current(id)) {
                    utility.logout();
                }
            } catch (e) {
                alert(e);
            }
        },

        /**
         * @returns {Promise<Session[]>}
         */
        list_sessions: async function () {
            try {
                return await api.user.list_sessions(cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },

        /**
         * @param {number|string} sessionId 
         * @returns {Promise}
         */
        invalidate_session: async function (sessionId) {
            try {
                await api.user.invalidate_session(sessionId, cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },

        /**
         * @param {string} email 
         * @param {string} password 
         * @returns {Promise}
         */
        delete_account: async function (email, password) {
            try {
                await api.user.delete_account(email, password, cookies.getSession());
            } catch ({ error, code }) {
                if (code == 401) {
                    utility.logout();
                } else {
                    alert(error);
                }
            }
        },
    },



    /**
     * @param {Element} item 
     * @returns {Promise}
     */
    load_dynamic_content: async function (item) {
        if (item == null) return;
        const promises = [];
        for (const e of item.querySelectorAll("[type='text/x-html-template']")) {
            promises.push((async _ => {
                try {
                    const result = await fetch(e.getAttribute("src") /*, { cache: "force-cache" } */);
                    e.innerHTML = await result.text();
                } catch (err) {
                    e.innerHTML = JSON.stringify(err);
                }
                nodeScriptReplace(e);

                await page.load_dynamic_content(e.nextElementSibling);
            })());
        }
        
        for (const e of item.querySelectorAll("script[type='text/x-handlebars-template']")) {
            promises.push((async _ => {
                try {
                    if(e.hasAttribute("partials"))
                        for(const partial of eval(e.getAttribute("partials"))){
                            if(partial in Handlebars.partials)continue;
                            const result = await fetch("/partials/"+partial/*, { cache: "force-cache" } */);
                            Handlebars.registerPartial(partial, await result.text());
                        }
                    const template = Handlebars.compile(e.innerHTML);
                    e.nextElementSibling.innerHTML = template({});
                    if(e.hasAttributes("src")){
                        result = eval(e.getAttribute("src"));
                        if(result instanceof Promise)
                            result = await result;
                        e.nextElementSibling.innerHTML = template(result);
                    }
                } catch (err) {
                    console.log(e);
                    console.log(err);
                    e.nextElementSibling.innerHTML = "ERROR: " + JSON.stringify(err, 2, null);
                }

                await page.load_dynamic_content(e.nextElementSibling);
            })());
        }
        await Promise.all(promises)
    },
};

function nodeScriptReplace(node) {
    if(node.tagName === 'SCRIPT'){
        node.parentNode.replaceChild(nodeScriptClone(node) , node);
    }else{
        var i = -1;
        var children = node.childNodes;
        while(++i < children.length){
            nodeScriptReplace(children[i]);
        }
    }

    return node;
}
function nodeScriptClone(node){
    var script  = document.createElement("script");
    script.text = node.innerHTML;

    var i = -1;
    var attrs = node.attributes, attr;
    while( ++i < attrs.length ) {                                    
        script.setAttribute((attr = attrs[i]).name, attr.value);
    }
    return script;
}

document.addEventListener('dynamic_content_finished', () => {
    console.log("Dynamic content finished loading");
    document.body.style = "";
});


document.addEventListener('DOMContentLoaded', async () => {
    document.body.style = "display:none";
    if (typeof Handlebars !== 'undefined') {
        Handlebars.registerHelper("raw-helper", function (options) {
            return options.fn();
        });
        Handlebars.registerHelper('formatTime', function (millis) {
            const date = new Date(millis);
            const hours = date.getHours();
            const minutes = date.getMinutes().toString().padStart(2, '0');
            const ampm = hours >= 12 ? 'pm' : 'am';
            const formattedHours = hours % 12 || 12; // Convert 24-hour to 12-hour format

            return `${date.toLocaleString('default', { month: 'long' })} ${date.getDate()}, ${date.getFullYear()} @ ${formattedHours.toString().padStart(2, '0')}:${minutes} ${ampm}`;
        });
        Handlebars.registerHelper('logColor', function(level) {
            const colors = { "SEVERE": "red", "WARNING": "yellow", "INFO": "blue", "CONFIG": "grey" };
            return colors[level] || "grey";
        });
        Handlebars.registerHelper('eq', function(a, b) {
            return (a === b);
        });
        Handlebars.registerHelper('gt', function(a, b) {
            return (a > b);
        });
        Handlebars.registerHelper('gte', function(a, b) {
            return (a >= b);
        });
        Handlebars.registerHelper('lt', function(a, b) {
            return (a < b);
        });
        Handlebars.registerHelper('lte', function(a, b) {
            return (a <= b);
        });
        Handlebars.registerHelper('ne', function(a, b) {
            return (a !== b);
        });
        Handlebars.registerHelper('json', function(context) {
            return JSON.stringify(context);
        });
        
    }

    await page.load_dynamic_content(document);
    for (let e of document.querySelectorAll("div[onclick]")) {
        e.addEventListener("click", ev => {
            eval(e.getAttribute("onclick"));
        })
    }
    document.dispatchEvent(new Event("dynamic_content_finished"));
});

function gen_qr(data, size=150){
    return `https://api.qrserver.com/v1/create-qr-code/?size=${size}x${size}&data=`+encodeURI(data);
}