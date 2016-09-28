const fetch = require('isomorphic-fetch')

const SIDECAR = {
    uri: 'http://localhost:8741'
}

const USER_SERVICE = 'user-service'
const CONFIG_SERVER = 'config-server'

exports.getConfigServerInfo = () => fetch(`${SIDECAR.uri}/hosts/${CONFIG_SERVER}`).then((resp)=>resp.json())

exports.getConfig = (configName) => fetch(`${SIDECAR.uri}/${CONFIG_SERVER}/${configName}.json`).then((resp)=>resp.json())

exports.getUserById = (id) => fetch(`${SIDECAR.uri}/${USER_SERVICE}/${id}`).then((resp)=>resp.json())
