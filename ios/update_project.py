import os

# Complete hierarchy of files for Nivya iOS Xcode Project
# group_name -> { 'path': group_path, 'subgroups': {...}, 'files': [...] }

id_counter = 100

def get_id(prefix):
    global id_counter
    id_counter += 1
    return f"{prefix}{id_counter:04X}2C8D000100000001"

files_structure = {
    'App': [
        ('AppState.swift', 'sourcecode.swift', True, False),
        ('LaunchScreen.storyboard', 'file.storyboard', False, True),
        ('NivyaApp.swift', 'sourcecode.swift', True, False),
    ],
    'Core': {
        'Network': [
            ('APIEndpoint.swift', 'sourcecode.swift', True, False),
            ('NetworkClient.swift', 'sourcecode.swift', True, False),
            ('NetworkError.swift', 'sourcecode.swift', True, False),
            ('WebSocketClient.swift', 'sourcecode.swift', True, False),
        ],
        'Permissions': [
            ('PermissionManager.swift', 'sourcecode.swift', True, False),
        ],
        'Security': [
            ('KeychainManager.swift', 'sourcecode.swift', True, False),
        ],
        'Storage': [
            ('AppPreferences.swift', 'sourcecode.swift', True, False),
        ],
        'Theme': [
            ('NivyaColors.swift', 'sourcecode.swift', True, False),
        ]
    },
    'Data': {
        'Models': [
            ('AuthModels.swift', 'sourcecode.swift', True, False),
            ('ConvocationModels.swift', 'sourcecode.swift', True, False),
            ('PairingModels.swift', 'sourcecode.swift', True, False),
            ('SessionModels.swift', 'sourcecode.swift', True, False),
            ('TelemetryModels.swift', 'sourcecode.swift', True, False),
        ],
        'Repositories': [
            ('AuthRepository.swift', 'sourcecode.swift', True, False),
            ('ConvocationRepository.swift', 'sourcecode.swift', True, False),
            ('PairingRepository.swift', 'sourcecode.swift', True, False),
            ('SessionRepository.swift', 'sourcecode.swift', True, False),
            ('TelemetryRepository.swift', 'sourcecode.swift', True, False),
        ]
    },
    'Features': {
        'Auth': [
            ('LoginView.swift', 'sourcecode.swift', True, False),
            ('RegisterView.swift', 'sourcecode.swift', True, False),
        ],
        'ChildDashboard': [
            ('ChildDashboardView.swift', 'sourcecode.swift', True, False),
        ],
        'Convocation': [
            ('ConvocationView.swift', 'sourcecode.swift', True, False),
        ],
        'Pairing': [
            ('ChildPairingView.swift', 'sourcecode.swift', True, False),
            ('ParentPairingView.swift', 'sourcecode.swift', True, False),
        ],
        'ParentDashboard': [
            ('ParentDashboardView.swift', 'sourcecode.swift', True, False),
        ],
        'Role': [
            ('RoleSelectionView.swift', 'sourcecode.swift', True, False),
        ],
        'Settings': [
            ('ChildSettingsView.swift', 'sourcecode.swift', True, False),
            ('ParentSettingsView.swift', 'sourcecode.swift', True, False),
            ('ProtectedDisconnectView.swift', 'sourcecode.swift', True, False),
            ('SettingsView.swift', 'sourcecode.swift', True, False),
        ],
        'Splash': [
            ('SplashScreenView.swift', 'sourcecode.swift', True, False),
        ]
    },
    'Services': [
        ('BatteryService.swift', 'sourcecode.swift', True, False),
        ('DeviceHealthService.swift', 'sourcecode.swift', True, False),
        ('LocationService.swift', 'sourcecode.swift', True, False),
        ('NetworkMonitoringService.swift', 'sourcecode.swift', True, False),
        ('NotificationService.swift', 'sourcecode.swift', True, False),
        ('TelemetrySyncService.swift', 'sourcecode.swift', True, False),
    ],
    'Resources': [
        ('Assets.xcassets', 'folder.assetcatalog', False, True),
        ('Info.plist', 'text.plist.xml', False, False),
    ]
}

test_files = [
    'AuthenticationTests.swift',
    'ChildDashboardOrderTests.swift',
    'CrackFreakTests.swift',
    'CrossPlatformParentIPhoneChildAndroidTests.swift',
    'PairingAndReloginTests.swift',
    'ProtectedDisconnectTests.swift'
]

build_files = []
file_refs = []
groups = []

sources_build_phase_files = []
resources_build_phase_files = []
test_sources_build_phase_files = []

def process_level(name, node, path_name=None):
    group_id = get_id('G100')
    child_ids = []
    
    if isinstance(node, list):
        for fname, ftype, is_src, is_res in node:
            fr_id = get_id('F100')
            file_refs.append(f"\t\t{fr_id} /* {fname} */ = {{isa = PBXFileReference; lastKnownFileType = {ftype}; path = {fname}; sourceTree = \"<group>\"; }};")
            child_ids.append(f"{fr_id} /* {fname} */")
            
            if is_src:
                bf_id = get_id('B100')
                build_files.append(f"\t\t{bf_id} /* {fname} in Sources */ = {{isa = PBXBuildFile; fileRef = {fr_id} /* {fname} */; }};")
                sources_build_phase_files.append(f"\t\t\t\t{bf_id} /* {fname} in Sources */,")
            elif is_res:
                bf_id = get_id('B100')
                build_files.append(f"\t\t{bf_id} /* {fname} in Resources */ = {{isa = PBXBuildFile; fileRef = {fr_id} /* {fname} */; }};")
                resources_build_phase_files.append(f"\t\t\t\t{bf_id} /* {fname} in Resources */,")
    elif isinstance(node, dict):
        for sub_name, sub_node in node.items():
            sub_grp_id = process_level(sub_name, sub_node, sub_name)
            child_ids.append(f"{sub_grp_id} /* {sub_name} */")
            
    children_str = "\n".join([f"\t\t\t\t{cid}," for cid in child_ids])
    p_str = f"path = {path_name}; " if path_name else ""
    groups.append(f"""\t\t{group_id} /* {name} */ = {{
\t\t\tisa = PBXGroup;
\t\t\tchildren = (
{children_str}
\t\t\t);
\t\t\t{p_str}sourceTree = \"<group>\";
\t\t}};""")
    return group_id

def generate_pbxproj():
    global id_counter, build_files, file_refs, groups
    global sources_build_phase_files, resources_build_phase_files, test_sources_build_phase_files
    
    id_counter = 100
    build_files = []
    file_refs = []
    groups = []
    sources_build_phase_files = []
    resources_build_phase_files = []
    test_sources_build_phase_files = []
    
    # Process Nivya group
    nivya_group_id = process_level('Nivya', files_structure, 'Nivya')

    # Process NivyaTests group
    test_group_id = get_id('G100')
    test_child_ids = []
    for tf in test_files:
        fr_id = get_id('F100')
        file_refs.append(f"\t\t{fr_id} /* {tf} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {tf}; sourceTree = \"<group>\"; }};")
        test_child_ids.append(f"{fr_id} /* {tf} */")
        bf_id = get_id('B100')
        build_files.append(f"\t\t{bf_id} /* {tf} in Sources */ = {{isa = PBXBuildFile; fileRef = {fr_id} /* {tf} */; }};")
        test_sources_build_phase_files.append(f"\t\t\t\t{bf_id} /* {tf} in Sources */,")

    test_children_str = "\n".join([f"\t\t\t\t{cid}," for cid in test_child_ids])
    groups.append(f"""\t\t{test_group_id} /* NivyaTests */ = {{
\t\t\tisa = PBXGroup;
\t\t\tchildren = (
{test_children_str}
\t\t\t);
\t\t\tpath = NivyaTests;
\t\t\tsourceTree = \"<group>\";
\t\t}};""")

    # Products
    app_product_id = "A10000102C8D000100000001"
    test_product_id = "B10000202C8D000100000001"

    file_refs.append(f"\t\t{app_product_id} /* Nivya.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = Nivya.app; sourceTree = BUILT_PRODUCTS_DIR; }};")
    file_refs.append(f"\t\t{test_product_id} /* NivyaTests.xctest */ = {{isa = PBXFileReference; explicitFileType = wrapper.cfbundle; includeInIndex = 0; path = NivyaTests.xctest; sourceTree = BUILT_PRODUCTS_DIR; }};")

    products_group_id = get_id('G100')
    groups.append(f"""\t\t{products_group_id} /* Products */ = {{
\t\t\tisa = PBXGroup;
\t\t\tchildren = (
\t\t\t\t{app_product_id} /* Nivya.app */,
\t\t\t\t{test_product_id} /* NivyaTests.xctest */,
\t\t\t);
\t\t\tname = Products;
\t\t\tsourceTree = \"<group>\";
\t\t}};""")

    # Main Group
    main_group_id = "A10000122C8D000100000001"
    groups.append(f"""\t\t{main_group_id} = {{
\t\t\tisa = PBXGroup;
\t\t\tchildren = (
\t\t\t\t{nivya_group_id} /* Nivya */,
\t\t\t\t{test_group_id} /* NivyaTests */,
\t\t\t\t{products_group_id} /* Products */,
\t\t\t);
\t\t\tsourceTree = \"<group>\";
\t\t}};""")

    # Build phases
    app_sources_id = "A10000172C8D000100000001"
    app_frameworks_id = "A10000112C8D000100000001"
    app_resources_id = "A10000182C8D000100000001"

    test_sources_id = "B10000252C8D000100000001"
    test_frameworks_id = "B10000212C8D000100000001"

    # Targets
    app_target_id = "A10000152C8D000100000001"
    test_target_id = "B10000232C8D000100000001"
    project_id = "A10000192C8D000100000001"

    app_config_list_id = "A10000162C8D000100000001"
    test_config_list_id = "B10000242C8D000100000001"
    project_config_list_id = "A10000202C8D000100000001"

    # Construct PBXProject content
    output = []
    output.append("// !$*UTF8*$!")
    output.append("{")
    output.append("\tarchiveVersion = 1;")
    output.append("\tclasses = {")
    output.append("\t};")
    output.append("\tobjectVersion = 56;")
    output.append("\tobjects = {")
    output.append("")

    # PBXBuildFile section
    output.append("/* Begin PBXBuildFile section */")
    for bf in build_files:
        output.append(bf)
    output.append("/* End PBXBuildFile section */")
    output.append("")

    # PBXFileReference section
    output.append("/* Begin PBXFileReference section */")
    for fr in file_refs:
        output.append(fr)
    output.append("/* End PBXFileReference section */")
    output.append("")

    # PBXFrameworksBuildPhase section
    output.append("/* Begin PBXFrameworksBuildPhase section */")
    output.append(f"""\t\t{app_frameworks_id} /* Frameworks */ = {{
\t\t\tisa = PBXFrameworksBuildPhase;
\t\t\tbuildActionMask = 2147483647;
\t\t\tfiles = (
\t\t\t);
\t\t\trunOnlyForDeploymentPostprocessing = 0;
\t\t}};""")
    output.append(f"""\t\t{test_frameworks_id} /* Frameworks */ = {{
\t\t\tisa = PBXFrameworksBuildPhase;
\t\t\tbuildActionMask = 2147483647;
\t\t\tfiles = (
\t\t\t);
\t\t\trunOnlyForDeploymentPostprocessing = 0;
\t\t}};""")
    output.append("/* End PBXFrameworksBuildPhase section */")
    output.append("")

    # PBXGroup section
    output.append("/* Begin PBXGroup section */")
    for grp in groups:
        output.append(grp)
    output.append("/* End PBXGroup section */")
    output.append("")

    # PBXNativeTarget section
    output.append("/* Begin PBXNativeTarget section */")
    output.append(f"""\t\t{app_target_id} /* Nivya */ = {{
\t\t\tisa = PBXNativeTarget;
\t\t\tbuildConfigurationList = {app_config_list_id} /* Build configuration list for PBXNativeTarget \"Nivya\" */;
\t\t\tbuildPhases = (
\t\t\t\t{app_sources_id} /* Sources */,
\t\t\t\t{app_frameworks_id} /* Frameworks */,
\t\t\t\t{app_resources_id} /* Resources */,
\t\t\t);
\t\t\tbuildRules = (
\t\t\t);
\t\t\tdependencies = (
\t\t\t);
\t\t\tname = Nivya;
\t\t\tproductName = Nivya;
\t\t\tproductReference = {app_product_id} /* Nivya.app */;
\t\t\tproductType = \"com.apple.product-type.application\";
\t\t}};""")
    output.append(f"""\t\t{test_target_id} /* NivyaTests */ = {{
\t\t\tisa = PBXNativeTarget;
\t\t\tbuildConfigurationList = {test_config_list_id} /* Build configuration list for PBXNativeTarget \"NivyaTests\" */;
\t\t\tbuildPhases = (
\t\t\t\t{test_sources_id} /* Sources */,
\t\t\t\t{test_frameworks_id} /* Frameworks */,
\t\t\t);
\t\t\tbuildRules = (
\t\t\t);
\t\t\tdependencies = (
\t\t\t);
\t\t\tname = NivyaTests;
\t\t\tproductName = NivyaTests;
\t\t\tproductReference = {test_product_id} /* NivyaTests.xctest */;
\t\t\tproductType = \"com.apple.product-type.bundle.unit-test\";
\t\t}};""")
    output.append("/* End PBXNativeTarget section */")
    output.append("")

    # PBXProject section
    output.append("/* Begin PBXProject section */")
    output.append(f"""\t\t{project_id} /* Project object */ = {{
\t\t\tisa = PBXProject;
\t\t\tattributes = {{
\t\t\t\tBuildIndependentTargetsInParallel = 1;
\t\t\t\tLastSwiftUpdateCheck = 1520;
\t\t\t\tLastUpgradeCheck = 1520;
\t\t\t\tTargetAttributes = {{
\t\t\t\t\t{app_target_id} = {{
\t\t\t\t\t\tCreatedOnToolsVersion = 15.2;
\t\t\t\t\t}};
\t\t\t\t\t{test_target_id} = {{
\t\t\t\t\t\tCreatedOnToolsVersion = 15.2;
\t\t\t\t\t\tTestTargetID = {app_target_id};
\t\t\t\t\t}};
\t\t\t\t}};
\t\t\t}};
\t\t\tbuildConfigurationList = {project_config_list_id} /* Build configuration list for PBXProject \"Nivya\" */;
\t\t\tcompatibilityVersion = \"Xcode 14.0\";
\t\t\tdevelopmentRegion = en;
\t\t\thasScannedForEncodings = 0;
\t\t\tknownRegions = (
\t\t\t\ten,
\t\t\t\tBase,
\t\t\t);
\t\t\tmainGroup = {main_group_id};
\t\t\tproductRefGroup = {products_group_id} /* Products */;
\t\t\tprojectDirPath = \"\";
\t\t\tprojectRoot = \"\";
\t\t\ttargets = (
\t\t\t\t{app_target_id} /* Nivya */,
\t\t\t\t{test_target_id} /* NivyaTests */,
\t\t\t);
\t\t}};""")
    output.append("/* End PBXProject section */")
    output.append("")

    # PBXResourcesBuildPhase section
    output.append("/* Begin PBXResourcesBuildPhase section */")
    res_str = "\n".join(resources_build_phase_files)
    output.append(f"""\t\t{app_resources_id} /* Resources */ = {{
\t\t\tisa = PBXResourcesBuildPhase;
\t\t\tbuildActionMask = 2147483647;
\t\t\tfiles = (
{res_str}
\t\t\t);
\t\t\trunOnlyForDeploymentPostprocessing = 0;
\t\t}};""")
    output.append("/* End PBXResourcesBuildPhase section */")
    output.append("")

    # PBXSourcesBuildPhase section
    output.append("/* Begin PBXSourcesBuildPhase section */")
    src_str = "\n".join(sources_build_phase_files)
    test_src_str = "\n".join(test_sources_build_phase_files)
    output.append(f"""\t\t{app_sources_id} /* Sources */ = {{
\t\t\tisa = PBXSourcesBuildPhase;
\t\t\tbuildActionMask = 2147483647;
\t\t\tfiles = (
{src_str}
\t\t\t);
\t\t\trunOnlyForDeploymentPostprocessing = 0;
\t\t}};""")
    output.append(f"""\t\t{test_sources_id} /* Sources */ = {{
\t\t\tisa = PBXSourcesBuildPhase;
\t\t\tbuildActionMask = 2147483647;
\t\t\tfiles = (
{test_src_str}
\t\t\t);
\t\t\trunOnlyForDeploymentPostprocessing = 0;
\t\t}};""")
    output.append("/* End PBXSourcesBuildPhase section */")
    output.append("")

    # XCBuildConfiguration section
    output.append("""/* Begin XCBuildConfiguration section */
\t\tA10000212C8D000100000001 /* Debug */ = {
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {
\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;
\t\t\t\tCLANG_ANALYZER_NONNULL = YES;
\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
\t\t\t\tCLANG_ENABLE_MODULES = YES;
\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;
\t\t\t\tDEBUG_INFORMATION_FORMAT = dwarf;
\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;
\t\t\t\tENABLE_TESTABILITY = YES;
\t\t\t\tGCC_DYNAMIC_NO_PIC = NO;
\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;
\t\t\t\tGCC_OPTIMIZATION_LEVEL = 0;
\t\t\t\tGCC_PREPROCESSOR_DEFINITIONS = (
\t\t\t\t\t"DEBUG=1",
\t\t\t\t\t"$(inherited)",
\t\t\t\t);
\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 17.0;
\t\t\t\tMTL_ENABLE_DEBUG_INFO = INCLUDE_SOURCE;
\t\t\t\tONLY_ACTIVE_ARCH = YES;
\t\t\t\tSDKROOT = iphoneos;
\t\t\t\tSWIFT_ACTIVE_COMPILATION_CONDITIONS = "DEBUG $(inherited)";
\t\t\t\tSWIFT_OPTIMIZATION_LEVEL = "-Onone";
\t\t\t\tSWIFT_VERSION = 5.0;
\t\t\t};
\t\t\tname = Debug;
\t\t};
\t\tA10000222C8D000100000001 /* Release */ = {
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {
\t\t\t\tALWAYS_SEARCH_USER_PATHS = NO;
\t\t\t\tCLANG_ANALYZER_NONNULL = YES;
\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = "gnu++20";
\t\t\t\tCLANG_ENABLE_MODULES = YES;
\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;
\t\t\t\tDEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
\t\t\t\tENABLE_NS_ASSERTIONS = NO;
\t\t\t\tENABLE_STRICT_OBJC_MSGSEND = YES;
\t\t\t\tGCC_NO_COMMON_BLOCKS = YES;
\t\t\t\tIPHONEOS_DEPLOYMENT_TARGET = 17.0;
\t\t\t\tMTL_ENABLE_DEBUG_INFO = NO;
\t\t\t\tSDKROOT = iphoneos;
\t\t\t\tSWIFT_COMPILATION_MODE = wholemodule;
\t\t\t\tSWIFT_VERSION = 5.0;
\t\t\t\tVALIDATE_PRODUCT = YES;
\t\t\t};
\t\t\tname = Release;
\t\t};
\t\tA10000232C8D000100000001 /* Debug */ = {
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {
\t\t\t\tASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
\t\t\t\tASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
\t\t\t\tCODE_SIGN_STYLE = Automatic;
\t\t\t\tCURRENT_PROJECT_VERSION = 1;
\t\t\t\tGENERATE_INFOPLIST_FILE = NO;
\t\t\t\tINFOPLIST_FILE = Nivya/Resources/Info.plist;
\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (
\t\t\t\t\t"$(inherited)",
\t\t\t\t\t"@executable_path/Frameworks",
\t\t\t\t);
\t\t\t\tMARKETING_VERSION = 1.0.0;
\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.nivya.ios;
\t\t\t\tPRODUCT_NAME = "$(TARGET_NAME)";
\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;
\t\t\t\tTARGETED_DEVICE_FAMILY = "1,2";
\t\t\t};
\t\t\tname = Debug;
\t\t};
\t\tA10000242C8D000100000001 /* Release */ = {
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {
\t\t\t\tASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
\t\t\t\tASSETCATALOG_COMPILER_GLOBAL_ACCENT_COLOR_NAME = AccentColor;
\t\t\t\tCODE_SIGN_STYLE = Automatic;
\t\t\t\tCURRENT_PROJECT_VERSION = 1;
\t\t\t\tGENERATE_INFOPLIST_FILE = NO;
\t\t\t\tINFOPLIST_FILE = Nivya/Resources/Info.plist;
\t\t\t\tLD_RUNPATH_SEARCH_PATHS = (
\t\t\t\t\t"$(inherited)",
\t\t\t\t\t"@executable_path/Frameworks",
\t\t\t\t);
\t\t\t\tMARKETING_VERSION = 1.0.0;
\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.nivya.ios;
\t\t\t\tPRODUCT_NAME = "$(TARGET_NAME)";
\t\t\t\tSWIFT_EMIT_LOC_STRINGS = YES;
\t\t\t\tTARGETED_DEVICE_FAMILY = "1,2";
\t\t\t};
\t\t\tname = Release;
\t\t};
\t\tB10000262C8D000100000001 /* Debug */ = {
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {
\t\t\t\tALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES = YES;
\t\t\t\tBUNDLE_LOADER = "$(TEST_HOST)";
\t\t\t\tCODE_SIGN_STYLE = Automatic;
\t\t\t\tCURRENT_PROJECT_VERSION = 1;
\t\t\t\tGENERATE_INFOPLIST_FILE = YES;
\t\t\t\tMARKETING_VERSION = 1.0;
\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.nivya.ios.NivyaTests;
\t\t\t\tPRODUCT_NAME = "$(TARGET_NAME)";
\t\t\t\tSWIFT_VERSION = 5.0;
\t\t\t\tTEST_HOST = "$(BUILT_PRODUCTS_DIR)/Nivya.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/Nivya";
\t\t\t};
\t\t\tname = Debug;
\t\t};
\t\tB10000272C8D000100000001 /* Release */ = {
\t\t\tisa = XCBuildConfiguration;
\t\t\tbuildSettings = {
\t\t\t\tALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES = YES;
\t\t\t\tBUNDLE_LOADER = "$(TEST_HOST)";
\t\t\t\tCODE_SIGN_STYLE = Automatic;
\t\t\t\tCURRENT_PROJECT_VERSION = 1;
\t\t\t\tGENERATE_INFOPLIST_FILE = YES;
\t\t\t\tMARKETING_VERSION = 1.0;
\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = com.nivya.ios.NivyaTests;
\t\t\t\tPRODUCT_NAME = "$(TARGET_NAME)";
\t\t\t\tSWIFT_VERSION = 5.0;
\t\t\t\tTEST_HOST = "$(BUILT_PRODUCTS_DIR)/Nivya.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/Nivya";
\t\t\t};
\t\t\tname = Release;
\t\t};
/* End XCBuildConfiguration section */
""")

    # XCConfigurationList section
    output.append(f"""/* Begin XCConfigurationList section */
\t\t{app_config_list_id} /* Build configuration list for PBXNativeTarget \"Nivya\" */ = {{
\t\t\tisa = XCConfigurationList;
\t\t\tbuildConfigurations = (
\t\t\t\tA10000232C8D000100000001 /* Debug */,
\t\t\t\tA10000242C8D000100000001 /* Release */,
\t\t\t);
\t\t\tdefaultConfigurationIsVisible = 0;
\t\t\tdefaultConfigurationName = Release;
\t\t}};
\t\t{test_config_list_id} /* Build configuration list for PBXNativeTarget \"NivyaTests\" */ = {{
\t\t\tisa = XCConfigurationList;
\t\t\tbuildConfigurations = (
\t\t\t\tB10000262C8D000100000001 /* Debug */,
\t\t\t\tB10000272C8D000100000001 /* Release */,
\t\t\t);
\t\t\tdefaultConfigurationIsVisible = 0;
\t\t\tdefaultConfigurationName = Release;
\t\t}};
\t\t{project_config_list_id} /* Build configuration list for PBXProject \"Nivya\" */ = {{
\t\t\tisa = XCConfigurationList;
\t\t\tbuildConfigurations = (
\t\t\t\tA10000212C8D000100000001 /* Debug */,
\t\t\t\tA10000222C8D000100000001 /* Release */,
\t\t\t);
\t\t\tdefaultConfigurationIsVisible = 0;
\t\t\tdefaultConfigurationName = Release;
\t\t}};
/* End XCConfigurationList section */
\t}};
\trootObject = {project_id} /* Project object */;
}}
""")

    out_content = "\n".join(output)
    proj_path = os.path.join(os.path.dirname(__file__), 'Nivya.xcodeproj', 'project.pbxproj')
    with open(proj_path, 'wb') as f:
        f.write(out_content.encode('utf-8'))
    print(f"Generated {proj_path} with {len(build_files)} build files and {len(file_refs)} file refs.")

if __name__ == '__main__':
    generate_pbxproj()
