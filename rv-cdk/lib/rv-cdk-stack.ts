import * as apigwv2 from '@aws-cdk/aws-apigatewayv2-alpha';
import * as apigwv2_integrations from '@aws-cdk/aws-apigatewayv2-integrations-alpha';

import * as cdk from 'aws-cdk-lib';
import {aws_s3 as s3, Duration} from 'aws-cdk-lib';
import {aws_lambda as lambda} from 'aws-cdk-lib';
import {aws_cloudfront as cloudfront} from 'aws-cdk-lib';
import {aws_cloudfront_origins as cloudfront_origins} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import * as path from "path";
import {Architecture} from "aws-cdk-lib/aws-lambda";

export class RfcViewerCdkStack extends cdk.Stack {
    constructor(scope: Construct, id: string, props?: cdk.StackProps) {
        super(scope, id, props);

        const lambdaFn = new lambda.DockerImageFunction(this, 'LambdaFunction2', {
            code: lambda.DockerImageCode.fromImageAsset(path.join(__dirname, '../../rv-searcher/')),
            timeout: Duration.seconds(20),
            memorySize: 1024,
            architecture: Architecture.ARM_64,
        });
        const lambdaAlias = lambdaFn.addAlias('prod');
        const autoScaling = lambdaAlias.addAutoScaling({maxCapacity: 5});
        autoScaling.scaleOnUtilization({utilizationTarget: 0.5});

        const httpApi = new apigwv2.HttpApi(this, 'HttpApi', {
            corsPreflight: {
                allowOrigins: ['*'],
                allowMethods: [apigwv2.CorsHttpMethod.ANY],
                maxAge: cdk.Duration.days(10),
            },
        });

        new apigwv2.HttpRoute(this, 'Visits', {
            httpApi: httpApi,
            routeKey: apigwv2.HttpRouteKey.DEFAULT,
            integration: new apigwv2_integrations.HttpLambdaIntegration(
                'DefaultIntegration', lambdaAlias),
        });

        // const cloudfrontDistribution = new cloudfront.Distribution(this, 'myDist', {
        //     defaultBehavior: {
        //         origin: new cloudfront_origins.HttpOrigin(httpApi.apiEndpoint.replace(/^https:\/\//gi, ""))
        //     },
        // });

        new s3.Bucket(this, 'MyFirstBucket', {
            versioned: true,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
            autoDeleteObjects: true
        });

        new cdk.CfnOutput(this, 'HttpApiUrl', {
            exportName: 'RfcViewerEndpoint',
            value: httpApi.apiEndpoint,
            description: 'RfcViewer API endpoint',
        });

        // new cdk.CfnOutput(this, 'CloudfrontUrl', {
        //     exportName: 'RfcViewerCloudfrontEndpoint',
        //     value: cloudfrontDistribution.domainName,
        //     description: 'RfcViewer API endpoint',
        // });
    }
}
