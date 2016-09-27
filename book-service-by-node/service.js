const fetch = require('isomorphic-fetch')

const SIDECAR = {
    uri: 'http://localhost:8741'
}

const USER_SERVICE = 'config-server'

exports.getConfigServerInfo = () => fetch(`${SIDECAR.uri}/hosts/${USER_SERVICE}`).then((resp)=>resp.json())

exports.getUserById = (id) => fetch(`${SIDECAR.uri}/${USER_SERVICE}/${id}`).then((resp)=>resp.json())
