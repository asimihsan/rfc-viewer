import * as apigwv2 from '@aws-cdk/aws-apigatewayv2-alpha';
import * as apigwv2_integrations from '@aws-cdk/aws-apigatewayv2-integrations-alpha';

import * as cdk from 'aws-cdk-lib';
import {
    aws_cloudfront as cloudfront,
    aws_cloudfront_origins as cloudfront_origins,
    aws_lambda as lambda,
    aws_s3 as s3,
    Duration
} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import * as path from "path";
import {Architecture} from "aws-cdk-lib/aws-lambda";
import {AllowedMethods, HttpVersion, PriceClass} from "aws-cdk-lib/aws-cloudfront";
import {HttpRouteKey} from "@aws-cdk/aws-apigatewayv2-alpha";

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

        // TODO make this configurable
        const autoScaling = lambdaAlias.addAutoScaling({maxCapacity: 5});
        autoScaling.scaleOnUtilization({utilizationTarget: 0.5});

        const defaultIntegration = new apigwv2_integrations.HttpLambdaIntegration(
            'DefaultIntegration', lambdaAlias);
        const httpApi = new apigwv2.HttpApi(this, 'HttpApi', {
            corsPreflight: {
                allowHeaders: ['Authorization', 'Content-Type', 'X-Amz-Date', 'X-Api-Key'],
                allowOrigins: ['*'],
                allowMethods: [apigwv2.CorsHttpMethod.ANY],
                maxAge: cdk.Duration.days(1),
            },
        });

        httpApi.addRoutes({
            integration: defaultIntegration,
            path: "/api/2022-11-07",
            methods: [apigwv2.HttpMethod.ANY],
        });

        const strippedEndpoint = cdk.Fn.parseDomainName(httpApi.apiEndpoint);

        // const cloudfrontDistribution = new cloudfront.Distribution(this, 'myDist', {
        //     defaultBehavior: {
        //         origin: new cloudfront_origins.HttpOrigin(strippedEndpoint),
        //         allowedMethods: AllowedMethods.ALLOW_ALL,
        //     },
        //     httpVersion: HttpVersion.HTTP2_AND_3,
        //     priceClass: PriceClass.PRICE_CLASS_200,
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

        new cdk.CfnOutput(this, 'StrippedEndpoint', {
            exportName: 'strippedEndpoint',
            value: strippedEndpoint,
            description: 'RfcViewer API stripped endpoint',
        });

        // new cdk.CfnOutput(this, 'CloudfrontUrl', {
        //     exportName: 'RfcViewerCloudfrontEndpoint',
        //     value: cloudfrontDistribution.domainName,
        //     description: 'RfcViewer API endpoint',
        // });
    }
}
