# Consul-KV-Builder
Jenkins plugin to read/write/delete K/V pairs from/to Consul K/V store.

To build with tests, you must supply the `HOST` constant in the `org.jenkinsci.plugins.consulkv.ConsulKVBuilderTest` class.

You may also need to supply the `ACL_ID` is the Consul is secured with an ACL.
