import * as apigwv2 from '@aws-cdk/aws-apigatewayv2-alpha';
import * as apigwv2_integrations from '@aws-cdk/aws-apigatewayv2-integrations-alpha';

import * as cdk from 'aws-cdk-lib';
import {
    aws_certificatemanager as acm,
    aws_cloudfront as cloudfront,
    aws_cloudfront_origins as cloudfront_origins,
    aws_lambda as lambda,
    aws_route53 as route53,
    aws_route53_targets as route53_targets,
    aws_s3 as s3,
    Duration
} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import * as path from "path";
import {Architecture} from "aws-cdk-lib/aws-lambda";
import {
    AllowedMethods, CachePolicy,
    HttpVersion,
    OriginAccessIdentity, OriginRequestPolicy,
    PriceClass,
    ResponseHeadersPolicy,
    ViewerProtocolPolicy
} from "aws-cdk-lib/aws-cloudfront";
import {BucketAccessControl} from "aws-cdk-lib/aws-s3";
import {BucketDeployment, Source} from "aws-cdk-lib/aws-s3-deployment";
import {S3Origin} from "aws-cdk-lib/aws-cloudfront-origins";

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

        const staticBucket = new s3.Bucket(this, 'StaticWebsite', {
            versioned: true,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
            autoDeleteObjects: true,
            accessControl: BucketAccessControl.PRIVATE,
        });

        const hostedZone = route53.HostedZone.fromLookup(this, 'SpecNinjaZone', {
            domainName: 'spec.ninja',
        });
        const myCertificate = new acm.DnsValidatedCertificate(this, 'SpecNinjaSiteCert', {
            domainName: 'www.spec.ninja',
            subjectAlternativeNames: [
                'spec.ninja',
            ],
            hostedZone: hostedZone,
            region: 'us-east-1',
        });

        const originAccessIdentity = new OriginAccessIdentity(this, 'OriginAccessIdentity');
        staticBucket.grantRead(originAccessIdentity);

        const strippedEndpoint = cdk.Fn.parseDomainName(httpApi.apiEndpoint);
        const cloudfrontDistribution = new cloudfront.Distribution(this, 'myDist', {
            httpVersion: HttpVersion.HTTP2_AND_3,
            priceClass: PriceClass.PRICE_CLASS_200,
            defaultRootObject: 'index.html',
            domainNames: ['spec.ninja', 'www.spec.ninja'],
            certificate: myCertificate,
            defaultBehavior: {
                origin: new S3Origin(staticBucket, {
                    originAccessIdentity: originAccessIdentity
                }),
                compress: true,
                allowedMethods: AllowedMethods.ALLOW_GET_HEAD_OPTIONS,
                responseHeadersPolicy: ResponseHeadersPolicy.CORS_ALLOW_ALL_ORIGINS_WITH_PREFLIGHT,
                viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
                cachePolicy: CachePolicy.CACHING_OPTIMIZED,
                originRequestPolicy: OriginRequestPolicy.CORS_S3_ORIGIN,
            },
            additionalBehaviors: {
                'api/*': {
                    origin: new cloudfront_origins.HttpOrigin(strippedEndpoint),
                    allowedMethods: AllowedMethods.ALLOW_ALL,
                    compress: true,
                    responseHeadersPolicy: ResponseHeadersPolicy.CORS_ALLOW_ALL_ORIGINS_WITH_PREFLIGHT,
                    viewerProtocolPolicy: ViewerProtocolPolicy.REDIRECT_TO_HTTPS,
                    cachePolicy: CachePolicy.CACHING_DISABLED,
                    originRequestPolicy: OriginRequestPolicy.CORS_CUSTOM_ORIGIN,
                }
            }
        });

        new route53.ARecord(this, 'AliasRecordRoot', {
            zone: hostedZone,
            target: route53.RecordTarget.fromAlias(
                new route53_targets.CloudFrontTarget(cloudfrontDistribution)),
        });
        new route53.ARecord(this, 'AliasRecordWww', {
            zone: hostedZone,
            recordName: 'www',
            target: route53.RecordTarget.fromAlias(
                new route53_targets.CloudFrontTarget(cloudfrontDistribution)),
        });

        new BucketDeployment(this, 'BucketDeployment', {
            destinationBucket: staticBucket,
            sources: [Source.asset(path.resolve(__dirname, '../../rv-web/build'))],
            memoryLimit: 2048,
            distribution: cloudfrontDistribution
        });

        new cdk.CfnOutput(this, 'HttpApiUrl', {
            exportName: 'RfcViewerEndpoint',
            value: httpApi.apiEndpoint,
            description: 'RfcViewer API endpoint',
        });

        new cdk.CfnOutput(this, 'StaticBucket', {
            exportName: 'StaticBucket',
            value: staticBucket.bucketName,
            description: 'RfcViewer web static bucket',
        });

        new cdk.CfnOutput(this, 'CloudfrontUrl', {
            exportName: 'RfcViewerCloudfrontEndpoint',
            value: cloudfrontDistribution.domainName,
            description: 'RfcViewer API endpoint',
        });
    }
}
