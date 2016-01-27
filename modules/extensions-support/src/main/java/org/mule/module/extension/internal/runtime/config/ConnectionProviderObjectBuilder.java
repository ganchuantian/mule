/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import org.mule.api.MuleException;
import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.extension.api.introspection.property.ConnectionHandlingTypeModelProperty;
import org.mule.internal.connection.CachedConnectionProviderWrapper;
import org.mule.internal.connection.ConnectionManagerAdapter;
import org.mule.internal.connection.PooledConnectionProviderWrapper;
import org.mule.module.extension.internal.runtime.ParameterGroupAwareObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

/**
 * Implementation of {@link ParameterGroupAwareObjectBuilder} which produces instances
 * of {@link ConnectionProviderModel}
 *
 * @since 4.0
 */
public final class ConnectionProviderObjectBuilder extends ParameterGroupAwareObjectBuilder<ConnectionProvider>
{

    private final ConnectionProviderModel providerModel;
    private final PoolingProfile poolingProfile;
    private boolean disableValidation;
    private RetryPolicyTemplate retryPolicyTemplate;
    private final ConnectionManagerAdapter connectionManager;

    /**
     * Creates a new instances which produces instances based on the given {@code providerModel} and
     * {@code resolverSet}
     *
     * @param providerModel     the {@link ConnectionProviderModel} which describes the instances to be produced
     * @param resolverSet       a {@link ResolverSet} to populate the values
     * @param connectionManager a {@link ConnectionManagerAdapter} to obtain the default {@link RetryPolicyTemplate} in case
     *                          of none is provided
     */
    public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet, ConnectionManagerAdapter connectionManager)
    {
        this(providerModel, resolverSet, null, false, null, connectionManager);
    }

    public ConnectionProviderObjectBuilder(ConnectionProviderModel providerModel, ResolverSet resolverSet, PoolingProfile poolingProfile, boolean disableValidation, RetryPolicyTemplate retryPolicyTemplate, ConnectionManagerAdapter connectionManager)
    {
        super(providerModel.getConnectionProviderFactory().getObjectType(), providerModel, resolverSet);
        this.providerModel = providerModel;
        this.poolingProfile = poolingProfile;
        this.disableValidation = disableValidation;
        this.retryPolicyTemplate = retryPolicyTemplate;
        this.connectionManager = connectionManager;
    }

    @Override
    public ConnectionProvider build(ResolverSetResult result) throws MuleException
    {
        ConnectionProvider provider = super.build(result);

        ConnectionHandlingTypeModelProperty connectionHandlingType = providerModel.getModelProperty(ConnectionHandlingTypeModelProperty.KEY);
        if (retryPolicyTemplate == null)
        {
            retryPolicyTemplate = connectionManager.getDefaultRetryPolicyTemplate();
        }

        if (connectionHandlingType != null)
        {
            if (connectionHandlingType.isPooled())
            {
                provider = new PooledConnectionProviderWrapper(provider, poolingProfile, disableValidation, retryPolicyTemplate);
            }

            if (connectionHandlingType.isCached())
            {
                provider = new CachedConnectionProviderWrapper(provider, disableValidation, retryPolicyTemplate);
            }
        }

        return provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnectionProvider instantiateObject()
    {
        return providerModel.getConnectionProviderFactory().newInstance();
    }
}
