package illinois.hack.openfashion.model

import java.util.HashMap


class User {

    private var fullName: String? = null
    private var photo: String? = null
    private var email: String? = null
    private var timestampJoined: HashMap<String, Any>? = null

    constructor() {}

    /**
     * Use this constructor to create new User.
     * Takes user name, email and timestampJoined as params
     *
     * @param timestampJoined
     */
    constructor(mFullName: String?, mPhoneNo: String?, mEmail: String?, timestampJoined: HashMap<String, Any>?) {
        this.fullName = mFullName
        this.photo = mPhoneNo
        this.email = mEmail
        this.timestampJoined = timestampJoined
    }
}