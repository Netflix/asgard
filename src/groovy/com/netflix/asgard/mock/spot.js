
callback({"vers": 0.01,"config": {"rate": "perhr","valueColumns": ["linux", "mswin"],"currencies": ["USD"],"regions": [{
    "region": "us-east",
    "footnotes": {
        "*" : "notAvailableForCCorCGPU"
    },
    "instanceTypes": [
        {
            "type": "stdSpot",
            "sizes": [
                {
                    "size": "sm",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.007"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.017"
                            }
                        }
                    ]
                },
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.013"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.033"
                            }
                        }
                    ]
                },
                {
                    "size": "lg",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.026"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.066"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.052"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.132"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "secgenstdSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.0575"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.1375"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.115"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.275"
                            }
                        }
                    ]
                },
            ]
        },
        {
            "type": "uSpot",
            "sizes": [
                {
                    "size": "u",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.003"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.006"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiMemSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.035"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.07"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.07"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.14"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.14"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.28"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiCPUSpot",
            "sizes": [
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.018"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.05"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.07"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.2"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterComputeI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.208"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxxxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.27"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterGPUI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.346"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        }
    ]
},{
    "region": "us-west-2",
    "footnotes": {
        "*" : "clusterOnlyInUSEast"
    },
    "instanceTypes": [
        {
            "type": "stdSpot",
            "sizes": [
                {
                    "size": "sm",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.01"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.026"
                            }
                        }
                    ]
                },
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.021"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.053"
                            }
                        }
                    ]
                },
                {
                    "size": "lg",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.042"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.106"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.083"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.211"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "secgenstdSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
            ]
        },
        {
            "type": "uSpot",
            "sizes": [
                {
                    "size": "u",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.01"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.009"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiMemSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.056"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.112"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.112"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.224"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.224"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.448"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiCPUSpot",
            "sizes": [
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.028"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.08"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.112"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.32"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterComputeI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxxxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.253"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterGPUI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        }
    ]
},{
    "region": "us-west",
    "footnotes": {
        "*" : "clusterOnlyInUSEast"
    },
    "instanceTypes": [
        {
            "type": "stdSpot",
            "sizes": [
                {
                    "size": "sm",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.01"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.02"
                            }
                        }
                    ]
                },
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.02"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.04"
                            }
                        }
                    ]
                },
                {
                    "size": "lg",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.04"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.08"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.08"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.16"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "secgenstdSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
            ]
        },
        {
            "type": "uSpot",
            "sizes": [
                {
                    "size": "u",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.004"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.007"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiMemSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.059"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.099"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.118"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.198"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.236"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.396"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiCPUSpot",
            "sizes": [
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.028"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.06"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.11"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.24"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterComputeI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxxxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterGPUI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        }
    ]
},{
    "region": "eu-ireland",
    "footnotes": {
        "*" : "notAvailableForCCorCGPU"
    },
    "instanceTypes": [
        {
            "type": "stdSpot",
            "sizes": [
                {
                    "size": "sm",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.016"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.032"
                            }
                        }
                    ]
                },
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.032"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.064"
                            }
                        }
                    ]
                },
                {
                    "size": "lg",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.064"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.128"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.128"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.256"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "secgenstdSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
            ]
        },
        {
            "type": "uSpot",
            "sizes": [
                {
                    "size": "u",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.006"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.011"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiMemSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.094"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.158"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.189"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.317"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.378"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.634"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiCPUSpot",
            "sizes": [
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.044"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.096"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.176"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.384"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterComputeI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxxxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.488"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterGPUI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.54"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        }
    ]
},{
    "region": "apac-sin",
    "footnotes": {
        "*" : "clusterOnlyInUSEast"
    },
    "instanceTypes": [
        {
            "type": "stdSpot",
            "sizes": [
                {
                    "size": "sm",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.01"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.02"
                            }
                        }
                    ]
                },
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.02"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.04"
                            }
                        }
                    ]
                },
                {
                    "size": "lg",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.04"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.08"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.08"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.16"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "secgenstdSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
            ]
        },
        {
            "type": "uSpot",
            "sizes": [
                {
                    "size": "u",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.004"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.007"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiMemSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.059"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.099"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.118"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.198"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.236"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.396"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiCPUSpot",
            "sizes": [
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.028"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.06"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.11"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.24"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterComputeI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxxxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterGPUI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        }
    ]
},{
    "region": "apac-tokyo",
    "footnotes": {
        "*" : "clusterOnlyInUSEast"
    },
    "instanceTypes": [
        {
            "type": "stdSpot",
            "sizes": [
                {
                    "size": "sm",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.017"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.035"
                            }
                        }
                    ]
                },
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.035"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.07"
                            }
                        }
                    ]
                },
                {
                    "size": "lg",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.067"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.141"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.134"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.282"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "secgenstdSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
            ]
        },
        {
            "type": "uSpot",
            "sizes": [
                {
                    "size": "u",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.007"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.015"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiMemSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.104"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.172"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.208"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.344"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.416"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.688"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiCPUSpot",
            "sizes": [
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.048"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.106"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.192"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.426"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterComputeI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxxxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterGPUI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        }
    ]
},{
    "region": "apac-syd",
    "footnotes": {
        "*" : "clusterOnlyInUSEast"
    },
    "instanceTypes": [
        {
            "type": "stdSpot",
            "sizes": [
                {
                    "size": "sm",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.01"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.02"
                            }
                        }
                    ]
                },
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.02"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.04"
                            }
                        }
                    ]
                },
                {
                    "size": "lg",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.04"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.08"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.08"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.16"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "secgenstdSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
            ]
        },
        {
            "type": "uSpot",
            "sizes": [
                {
                    "size": "u",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.004"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.007"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiMemSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.059"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.099"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.118"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.198"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.236"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.396"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiCPUSpot",
            "sizes": [
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.028"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.06"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.11"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.24"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterComputeI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxxxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterGPUI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        }
    ]
},{
    "region": "sa-east-1",
    "footnotes": {
        "*" : "clusterOnlyInUSEast"
    },
    "instanceTypes": [
        {
            "type": "stdSpot",
            "sizes": [
                {
                    "size": "sm",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.011"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.021"
                            }
                        }
                    ]
                },
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.022"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.042"
                            }
                        }
                    ]
                },
                {
                    "size": "lg",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.042"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.082"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.084"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.164"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "secgenstdSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A"
                            }
                        }
                    ]
                },
            ]
        },
        {
            "type": "uSpot",
            "sizes": [
                {
                    "size": "u",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.004"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.007"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiMemSpot",
            "sizes": [
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.062"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.097"
                            }
                        }
                    ]
                },
                {
                    "size": "xxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.123"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.193"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.246"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.386"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "hiCPUSpot",
            "sizes": [
                {
                    "size": "med",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.024"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.057"
                            }
                        }
                    ]
                },
                {
                    "size": "xl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "0.096"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "0.226"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterComputeI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                },
                {
                    "size": "xxxxxxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        },
        {
            "type": "clusterGPUI",
            "sizes": [
                {
                    "size": "xxxxl",
                    "valueColumns": [
                        {
                            "name": "linux",
                            "prices": {
                                "USD": "N/A*"
                            }
                        },
                        {
                            "name": "mswin",
                            "prices": {
                                "USD": "N/A*"
                            }
                        }
                    ]
                }
            ]
        }
    ]
}]}})
