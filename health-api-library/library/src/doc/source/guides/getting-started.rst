.. _guide-getting-started:

===============
Getting started
===============


First of all: Add the Library to your Build
===========================================

The Gini Pay API Library is provided as a aar archive. You can integrate it in your gradle based project by
adding it as dependency. In order to gain access to the aar file, you have to add the Gini Maven
repository to your build script.

.. code-block:: groovy

    repositories {
        maven {
            url "https://repo.gini.net/nexus/content/repositories/public"
        }
        ...
    }

    dependencies {
        implementation('net.gini:gini-pay-api-lib-android:1.0.2@aar'){
            transitive = true
        }
        ...
    }

Integrating the Gini Pay API Library
====================================


The Gini Pay API Library provides the ``Gini`` class which is a façade to all functionality of the library. We
recommend using an instance of this class singleton-like. By saying singleton-like we mean that you
somehow manage to create and keep one instance at application start. Instead of creating a new
instance every time when you need to interact with the Gini API, you reuse this instance. This has
the benefits that the library can reuse sessions between requests to the Gini API which may save a
noteworthy number of HTTP requests.

Creating the Gini instance
------------------------------

In order to create an instance of the ``Gini`` class, you need both your client id and your client
secret. If you don't have a client id and client secret yet, you need to contact us and we'll provide 
you with credential.

All requests to the Gini API are made on behalf of a user. This means particularly that all created
documents are bound to a specific user account. But since you are most likely only interested in the
results of the semantic document analysis and not in a cloud document storage system, the Gini API
has the feature of "anonymous users". This means that user accounts are created on the fly and the
user account is unknown to your application's user.

The following example describes how to use the Gini API in your application with such anonymous user
accounts. To use the Gini API, you must create an instance of the ``Gini`` class. The ``Gini``
instance is configured and created with the help of the ``SdkBuilder`` class. In this example, the
anonymous users are created with the email domain "example.com". An example of a username created
with this configuration would be ``550e8400-e29b-11d4-a716-446655440000@example.com``

.. code-block:: java

    import net.gini.android.GiniBuilder;
    import net.gini.android.DocumentTaskManager;
    
    ...
    
    // The Gini instance is a facade to all available managers of the library. Configure and
    // create the SDK with the SdkBuilder.
    Gini gini = new SdkBuilder(getContext(), "gini-client-id", "GiniClientSecret", "example.com")
            .build();
    // The DocumentTaskManager provides the high-level API to work with documents.
    DocumentTaskManager documentManager = gini.getDocumentTaskManager();


Congratulations, you successfully integrated the Gini Pay.

Public Key Pinning
==================

Since version 1.5.0 public key pinning is provided using the `Android Network Security Configuration
<https://developer.android.com/training/articles/security-config.html>`_ and `TrustKit
<https://github.com/datatheorem/TrustKit-Android>`_. The previous configuration through the
`SdkBuilder` was removed.

To use public key pinning you need to create an `Android network security configuration
<https://developer.android.com/training/articles/security-config.html>`_ xml file. This
configuration is supported natively on Android Nougat (API Level 24) and newer. For versions between
API Level 17 and 23 Gini Pay API Library relies on `TrustKit
<https://github.com/datatheorem/TrustKit-Android>`_. On API Levels 15 and 16 our own pinning
implementation is used.

We recommend reading the `Android Network Security Configuration
<https://developer.android.com/training/articles/security-config.html>`_ guide and the `TrustKit
limitations for API Levels 17 to 23 <https://github.com/datatheorem/TrustKit-Android#limitations>`_.

Configure Pinning
-----------------

The following sample configuration shows how to set the public key pin for the two domains Gini
Pay API Library uses by default (``pay-api.gini.net`` and ``user.gini.net``). It should be saved under
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

    If you set different base urls when instantiating Gini with the ``SdkBuilder`` make sure
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

Enable Pinning
--------------

For the library to know about the xml you need to set the xml resource id using the
``SdkBuilder#setNetworkSecurityConfigResId()`` method:

.. code-block:: java

    Gini gini = new SdkBuilder(getContext(), "gini-client-id", "GiniClientSecret", "example.com")
            .setNetworkSecurityConfigResId(R.xml.network_security_config)
            .build();

Extract Hash From gini.net
--------------------------

The current Gini API public key SHA256 hash digest in Base64 encoding can be extracted with the
following openssl commands:

.. code-block:: bash

    $ openssl s_client -servername gini.net -connect gini.net:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64

Extract Hash From Public Key
----------------------------

You can also extract the hash from a public key. The following example shows how to extract it from
a public key named ``gini.pub``:

.. code-block:: bash

    $ cat gini.pub | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64