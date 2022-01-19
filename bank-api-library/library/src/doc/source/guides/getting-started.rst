.. _guide-getting-started:

===============
Getting started
===============


First of all: Add the library to your build
===========================================

build.gradle:

.. code-block:: groovy

    dependencies {
        implementation 'net.gini.android:gini-bank-api-lib:1.0.3'
    }

Integrating the Gini Bank API Library
=======================================


The Gini Bank API Library provides the ``GiniBankAPI`` class which is a façade to all functionality of the library.
We recommend using a single instance of this class and avoid instantiating it each time you need to interact with the
Gini Bank API. You can reuse the instance either through your `Application` subclass or via a dependency injection
solution. This has the benefits that the library can reuse sessions between requests to the Gini Bank API which may
save a noteworthy number of HTTP requests.

Creating the GiniBankAPI instance
-----------------------------------

In order to create an instance of the ``GiniBankAPI`` class, you need both your client id and your client
secret. If you don't have a client id and client secret yet, you need to contact us and we'll provide 
you with credential.

All requests to the Gini Bank API are made on behalf of a user. This means particularly that all created
documents are bound to a specific user account. But since you are most likely only interested in the
results of the semantic document analysis and not in a cloud document storage system, the Gini Bank API
has the feature of *anonymous users*. This means that user accounts are created on the fly and the
user account is unknown to your application's user.

The following example describes how to use the Gini Bank API in your application with such anonymous user
accounts. To use the Gini Bank API, you must create an instance of the ``GiniBankAPI`` class. The ``GiniBankAPI``
instance is configured and created with the help of the ``GiniBankAPIBuilder`` class. In this example, the
anonymous users are created with the email domain "example.com". An example of a username created
with this configuration would be ``550e8400-e29b-11d4-a716-446655440000@example.com``

.. code-block:: java
    
    // The GiniBankAPI instance is a facade to all available managers of the library. Configure and
    // create the library with the GiniBankAPIBuilder.
    GiniBankAPI giniBankApi =
            new GiniBankAPIBuilder(getContext(), "gini-client-id", "GiniClientSecret", "example.com")
                    .build();

    // The DocumentTaskManager provides the high-level API to work with documents.
    DocumentTaskManager documentManager = giniBankApi.getDocumentTaskManager();

Public Key Pinning
==================

Public key pinning is provided using the `Android Network Security Configuration
<https://developer.android.com/training/articles/security-config.html>`_ and `TrustKit
<https://github.com/datatheorem/TrustKit-Android>`_.

To use public key pinning you can either create an `Android network security configuration
<https://developer.android.com/training/articles/security-config.html>`_ xml file or set a custom `TrustManager
<https://developer.android.com/reference/javax/net/ssl/TrustManager>`_ implementation.

The network security configuration is supported
natively on Android Nougat (API Level 24) and newer. For versions between API Level 21 and 23 the Gini SDK relies on
`TrustKit <https://github.com/datatheorem/TrustKit-Android>`_.

The custom ``TrustManager`` is supported on all Android versions.

We recommend reading the `Android Network Security Configuration
<https://developer.android.com/training/articles/security-config.html>`_ guide and the `TrustKit
limitations for API Levels 21 to 23 <https://github.com/datatheorem/TrustKit-Android#limitations>`_.

Configure Pinning
-----------------

The following sample configuration shows how to set the public key pin for the two domains. The Gini
Bank API Library uses by default (``pay-api.gini.net`` and ``user.gini.net``). It should be saved under
``res/xml/network_security_config.xml``:

.. code-block:: xml

    <?xml version="1.0" encoding="utf-8"?>
    <network-security-config>
        <domain-config>
            <trustkit-config
                disableDefaultReportUri="true"
                enforcePinning="true" />
            <domain includeSubdomains="false">pay-api.gini.net</domain>
            <pin-set>
                <!-- old *.gini.net public key-->
                <pin digest="SHA-256">yGLLyvZLo2NNXeBNKJwx1PlCtm+YEVU6h2hxVpRa4l4=</pin>
                <!-- new *.gini.net public key, active from around mid September 2018 -->
                <pin digest="SHA-256">cNzbGowA+LNeQ681yMm8ulHxXiGojHE8qAjI+M7bIxU=</pin>
            </pin-set>
            <domain-config>
                <trustkit-config
                    disableDefaultReportUri="true"
                    enforcePinning="true" />
                <domain includeSubdomains="false">user.gini.net</domain>
            </domain-config>
        </domain-config>
    </network-security-config>

.. note::

    If you set different base urls when instantiating with the ``GiniBankAPIBuilder``, then make sure
    you set matching domains in the network security configuration xml.

.. warning::

    The above digests serve as an example only. You should **always** create the digest yourself
    from the Gini API's public key and use that one (see `Extract Hash From gini.net`_). If you
    received a digest from us then **always** validate it by comparing it to the digest you created
    from the public key (see `Extract Hash From Public Key`_). Failing to validate a digest may lead
    to security vulnerabilities.

TrustKit
--------

The `TrustKit <https://github.com/datatheorem/TrustKit-Android>`_ configuration tag
``<trustkit-config>`` is required in order to disable TrustKit reporting and to enforce public key
pinning. This is important because without it TrustKit won't throw ``CertificateExceptions`` if the
local public keys didn't match any of the remote ones, effectively disabling pinning. The only
downside of enforcing pinning is that two public key hashes are required. In the example above we
create and used a "zero" key hash as a placeholder. Setting the same key hash twice won't help since
key hashes are stored in a set. Ideally you should use a backup public key hash as the second one.

In your ``AndroidManifest.xml`` you need to set the ``android:networkSecurityConfig`` attribute on
the ``<application>`` tag to point to the xml:

.. code-block:: xml

    <?xml version="1.0" encoding="utf-8"?>
    <manifest ...>
        ...
        <application android:networkSecurityConfig="@xml/network_security_config">
        ...
    </manifest>

Enable Pinning with a Network Security Configuration
----------------------------------------------------

For the library to know about the xml you need to set the xml resource id using the
``GiniBankAPIBuilder#setNetworkSecurityConfigResId()`` method:

.. code-block:: java

    GiniBankAPI giniBankApi = new GiniBankAPIBuilder(getContext(), "gini-client-id", "GiniClientSecret", "example.com")
            .setNetworkSecurityConfigResId(R.xml.network_security_config)
            .build();

Enable Pinning with a custom TrustManager implementation
--------------------------------------------------------

You can also take full control over which certificates to trust by passing your own ``TrustManager`` implementation
to the ``GiniBankAPIBuilder#setTrustManager()`` method:

.. code-block:: java

    GiniBankAPI giniBankApi = new GiniBankAPIBuilder(getContext(), "gini-client-id", "GiniClientSecret", "example.com")
            .setTrustManager(yourTrustManager)
            .build();

.. warning::

    Setting a custom ``TrustManager`` will override the network security configuration.

Extract Hash From gini.net
--------------------------

The current Gini Bank API public key SHA256 hash digest in Base64 encoding can be extracted with the
following openssl commands:

.. code-block:: bash

    $ openssl s_client -servername gini.net -connect gini.net:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64

Extract Hash From Public Key
----------------------------

You can also extract the hash from a public key. The following example shows how to extract it from
a public key named ``gini.pub``:

.. code-block:: bash

    $ cat gini.pub | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64