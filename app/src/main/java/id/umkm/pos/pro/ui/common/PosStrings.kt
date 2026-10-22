package id.umkm.pos.pro.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import id.umkm.pos.pro.core.model.AppLanguage
import id.umkm.pos.pro.core.model.CriticalAction

interface PosStrings {

    // Navigation
    val navCashier: String
    val navProducts: String
    val navTransactions: String
    val navReports: String
    val navMore: String

    // Common actions
    val actionSave: String
    val actionCancel: String
    val actionClose: String
    val actionOpen: String
    val actionShare: String
    val actionDelete: String
    val actionEdit: String
    val actionRetry: String
    val actionBack: String
    val actionAll: String
    val actionSearch: String
    val actionReset: String
    val actionDone: String
    val actionConfirm: String
    val actionApply: String
    val actionAdd: String
    val actionShowAll: String
    val actionClearSearch: String
    val actionPick: String

    // Common states
    val commonLoading: String
    val commonErrorTitle: String
    val commonOfflineNotice: String
    val commonNoCategory: String
    val commonRequired: String
    val commonOptional: String

    // Error messages
    val errorValidation: String
    val errorNotFound: String
    val errorStorage: String
    val errorHardware: String
    val errorNetwork: String
    val errorUnexpected: String

    // Dates and charts
    val dateMonthAbbreviations: List<String>
    val dateWeekdayAbbreviations: List<String>
    val chartSeriesTotal: String
    val chartEmpty: String
    val chartCompactThousand: String
    val chartCompactMillion: String

    // Cashier
    val cashierTitle: String
    val cashierSearchPlaceholder: String
    val cashierScanAction: String
    val scannerTitle: String
    val scannerManualLabel: String
    val scannerManualPlaceholder: String
    val scannerManualHint: String
    val scannerManualAdd: String
    val scannerManualUse: String
    val scannerCameraTitle: String
    val scannerPermissionTitle: String
    val scannerPermissionDetail: String
    val scannerPermissionGrant: String
    val scannerPermissionBlocked: String
    val scannerPermissionOpenSettings: String
    val scannerCameraError: String
    val scannerNoCameraDetail: String
    val scannerCameraScanning: String
    val scannerCameraSlowHint: String
    val scannerTorchEnable: String
    val scannerTorchDisable: String
    val scannerManualModeOn: String
    val scannerManualModeOff: String
    val scannerCameraAvailable: String
    val scannerCameraUnavailableDetail: String
    val scannerHidHint: String
    val settingsScannerTitle: String
    val settingsScannerMenuDescription: String
    val settingsScannerCameraSection: String
    val settingsScannerHidSection: String
    val settingsScannerHidDetail: String
    val settingsScannerHidToggle: String
    val settingsScannerHidToggleDetail: String
    val settingsScannerTestSection: String
    val settingsScannerTestHint: String
    val settingsScannerTestEmpty: String
    val settingsScannerTestResult: (String) -> String
    val cashierFilterAllCategories: String
    val cashierFilterInStockOnly: String
    val cashierFilterProducts: String
    val cashierFilterAllCategoriesLabel: String
    val cashierFilterReset: String
    val cashierCartTitle: String
    val cashierOpenCart: String
    val cashierEmptyCartTitle: String
    val cashierEmptyCartMsg: String
    val cashierClearCart: String
    val cashierPay: String
    val cashierTotal: String
    val cashierSubtotal: String
    val cashierDiscount: String
    val cashierTax: String
    val cashierRounding: String
    val cashierAmountReceived: String
    val cashierExactMoney: String
    val cashierQuickNominal: String
    val cashierQuickNominalOptions: String
    val cashierChange: String
    val cashierPaid: String
    val cashierRemainingDebt: String
    val cashierRemainingAmount: String
    val cashierRemainingDebtNeedsCustomer: String
    val cashierDueDate: String
    val cashierDueDateSet: (String) -> String
    val cashierSetDueDate: String
    val cashierSelectCustomer: String
    val cashierTransactionSuccess: String
    val cashierPrintReceipt: String
    val cashierShareReceipt: String
    val cashierShareReceiptImage: String
    val cashierNewSale: String
    val cashierLineItemOptions: String
    val cashierNegotiatedPrice: String
    val cashierNegotiatedPriceHint: String
    val cashierItemDiscount: String
    val cashierUnit: String
    val cashierBaseUnit: String
    val cashierUnitAndNegotiation: String
    val cashierApply: String
    val cashierStockEmpty: String
    val cashierStockInsufficient: String
    val cashierItemCountLine: (String, Int) -> String
    val cashierItemQuantityLine: (String, Int) -> String
    val cashierCartEmptyLine: String
    val cashierEditLineOptions: (String) -> String
    val cashierDecreaseLineQuantity: (String) -> String
    val cashierIncreaseLineQuantity: (String) -> String
    val cashierRemoveLine: (String) -> String
    val cashierLineDiscount: (String) -> String
    val cashierLineNegotiatedBadge: String
    val cashierLineWholesaleBadge: String
    val cashierAddProduct: (String) -> String
    val cashierDecreaseProduct: (String) -> String
    val cashierStockRemaining: (String) -> String
    val cashierProductCount: (Int) -> String
    val cashierNoProductsTitle: String
    val cashierNoProductsSearchTitle: String
    val cashierNoProductsSearchHint: String
    val cashierNoProductsKeywordHint: String
    val cashierNoProductsFilterHint: String
    val cashierNoProductsEmptyHint: String
    val cashierResetSearchAndFilter: String
    val cashierLoadingProducts: String
    val cashierPaymentTitle: String
    val cashierConfirmPayment: String
    val cashierPaymentMethodLabel: String
    val cashierAmountPaidNow: String
    val cashierDepositReceived: String
    val cashierDepositOptionalHint: String
    val cashierQrisNotConfigured: String
    val cashierPreparingQris: String
    val cashierQrisDescription: String
    val cashierAmountAutoFilled: String
    val cashierPaymentStatusLabel: String
    val cashierPaidInFull: (String) -> String
    val cashierPaymentMethodSummary: String
    val cashierItemCountValue: (Int) -> String
    val cashierProductNotFound: String
    val cashierBarcodeNotFound: (String) -> String
    val cashierStockEmptyNamed: (String) -> String
    val cashierStockInsufficientNamed: (String, String, String) -> String
    val cashierStockInsufficientShort: (String, String) -> String
    val cashierProductAdded: (String) -> String
    val cashierReceiptSent: String
    val cashierEnterAmountReceived: String
    val cashierAmountLessThanTotal: String
    val cashierInvalidNegotiatedPrice: String
    val cashierInvalidItemDiscount: String
    val cashierCustomerAdded: (String) -> String
    val cashierAddNewCustomer: String
    val cashierNewCustomerToggle: String
    val cashierCustomerNameLabel: String
    val cashierCustomerWhatsappLabel: String
    val cashierSaveAndSelectCustomer: String
    val cashierSearchCustomerPlaceholder: String
    val cashierNoCustomersHint: String
    val cashierCustomerNotFound: String
    val cashierCustomerDebtChip: (String) -> String

    // Shift & Cashier
    val cashierShiftOpenAction: String
    val cashierShiftSwitchAction: String
    val cashierShiftNotOpen: String
    val cashierShiftSupporting: (String, Int) -> String
    val cashierShiftRequired: String
    val shiftOpenTitle: String
    val shiftOpenSupporting: String
    val shiftCloseTitle: String
    val shiftCloseSupporting: (String) -> String
    val shiftCashierNameLabel: String
    val shiftCashierNamePlaceholder: String
    val shiftRecentCashiersLabel: String
    val shiftNoStaffTitle: String
    val shiftNoStaffHint: String
    val shiftManageStaffAction: String
    val shiftOperatorTab: String
    val shiftCloseTab: String
    val shiftSwitchOperatorAction: String
    val shiftOperatorSwitched: (String) -> String
    val shiftValidationStaff: String
    val shiftStartingCashLabel: String
    val shiftStartingCashHint: String
    val shiftNextCashierTitle: String
    val shiftNextCashierNameLabel: String
    val shiftNextCashierNamePlaceholder: String
    val shiftNextStartingCashLabel: String
    val shiftNextStartingCashHint: String
    val shiftSummaryTitle: String
    val shiftCashSalesLabel: String
    val shiftCashDebtPaymentsLabel: String
    val shiftCashRefundsLabel: String
    val shiftExpectedEndingCashLabel: String
    val shiftTransactionCountLabel: (Int) -> String
    val shiftActualEndingCashLabel: String
    val shiftActualEndingCashHint: String
    val shiftVarianceLabel: String
    val shiftVarianceBalanced: String
    val shiftVarianceOver: String
    val shiftVarianceShort: String
    val shiftVarianceAcknowledge: String
    val shiftSummaryLoading: String
    val shiftSummaryUnavailable: String
    val shiftValueEmpty: String
    val shiftOpenSubmit: String
    val shiftCloseAndSwitch: String
    val shiftCloseOnly: String
    val shiftValidationCashierName: String
    val shiftValidationStartingCash: String
    val shiftValidationActualCash: String
    val shiftValidationVarianceAck: String
    val shiftValidationNextCashier: String
    val shiftValidationNextStartingCash: String
    val shiftOpenSuccess: (String) -> String
    val shiftCloseSuccess: (String) -> String
    val shiftSwitchSuccess: (String, String) -> String
    val shiftCartGuardTitle: String
    val shiftCartGuardMessage: String
    val shiftCartGuardConfirm: String
    val shiftHistoryTitle: String
    val shiftHistoryAction: String
    val shiftHistorySubtitle: String
    val shiftHistoryEmptyTitle: String
    val shiftHistoryEmptyHint: String
    val shiftHistoryLoading: String
    val shiftHistoryStatusActive: String
    val shiftHistoryStatusClosed: String
    val shiftHistoryOpened: (String) -> String
    val shiftHistoryClosed: (String) -> String
    val shiftHistoryStartingCash: String
    val shiftHistoryExpectedCash: String
    val shiftHistoryActualCash: String
    val shiftHistoryVariance: String
    val shiftHistoryLiveNote: String

    // Payment Methods
    val paymentCash: String
    val paymentQris: String
    val paymentTransfer: String
    val paymentSplit: String
    val paymentKasbon: String
    val paymentSplitShort: String

    // Products
    val productsTitle: String
    val productsRegisteredCount: (Int) -> String
    val productsNewProduct: String
    val productsEditProduct: String
    val productsDeleteConfirmTitle: String
    val productsDeleteConfirmMsg: String
    val productsName: String
    val productsSku: String
    val productsBarcode: String
    val productsCategory: String
    val productsBasePrice: String
    val productsBaseHpp: String
    val productsStockQty: String
    val productsMinStockAlert: String
    val productsActive: String
    val productsActiveDesc: String
    val productsPhoto: String
    val productsPhotoPick: String
    val productsPhotoChange: String
    val productsPhotoRemove: String
    val productsPhotoChangeShort: String
    val productsPhotoWillBeRemoved: String
    val productsPricingAndUnits: String
    val productsOtherOptions: String
    val productsUnitName: String
    val productsUnitNameHint: String
    val productsConversionFactor: String
    val productsConversionFactorHint: String
    val productsUnitPrice: String
    val productsWholesaleMinQty: String
    val productsWholesalePrice: String
    val productsRequiredFieldNotice: String
    val productsAdd: String
    val productsAddAction: String
    val productsLoading: String
    val productsSearchPlaceholder: String
    val productsSort: String
    val productsSortNewest: String
    val productsSortNameAsc: String
    val productsSortLowestStock: String
    val productsFilterAll: (Int) -> String
    val productsFilterLowStock: (Int) -> String
    val productsFilterOutOfStock: (Int) -> String
    val productsFilterEmptyTitle: String
    val productsFilterEmptyHint: String
    val productsFilterShowAll: String
    val productsEmptyTitle: String
    val productsNotFoundTitle: String
    val productsEmptyHint: String
    val productsNotFoundHint: String
    val productsClearSearch: String
    val productsAddFirst: String
    val productsEditNamed: (String) -> String
    val productsMenuNamed: (String) -> String
    val productsStockSummary: (String) -> String
    val productsSectionBasics: String
    val productsSectionStock: String
    val productsFieldName: String
    val productsSkuAutoHint: String
    val productsFieldBaseUnit: String
    val productsBaseUnitHint: String
    val productsHppHint: String
    val productsLowStockHint: String
    val productsCategoryPickExisting: String
    val productsBaseUnitQuickPick: String
    val productsCloseForm: String
    val productsSaveChanges: String
    val productsSaveNew: String
    val productsImageFailed: String
    val productsSaveFailed: String
    val productsSaved: String
    val productsDeleted: String
    val productsNotFoundMessage: String
    val productsValidationSummary: (Int) -> String
    val productsValidationName: String
    val productsValidationBaseUnit: String
    val productsValidationBasePrice: String
    val productsValidationBaseHpp: String
    val productsValidationStock: String
    val productsValidationMinStockAlert: String
    val productsValidationUnitName: String
    val productsValidationConversionFactor: String
    val productsValidationUnitPrice: String
    val productsValidationWholesaleMinQty: String
    val productsValidationWholesalePrice: String

    // Stock Status
    val stockInStock: String
    val stockLowStock: String
    val stockOutOfStock: String
    val stockOversold: String

    // Customers & Kasbon
    val customersTitle: String
    val customersTitleShort: String
    val customersNewCustomer: String
    val customersAdd: String
    val customersAddAction: String
    val customersLoading: String
    val customersSearchPlaceholder: String
    val customersReceivableTotal: String
    val customersFilterAll: String
    val customersFilterUnpaid: String
    val customersFilterOverdue: String
    val customersFilterPaid: String
    val customersFilterEmpty: (String) -> String
    val customersFilterShowAll: String
    val customersRecordPayment: String
    val customersSelectDebt: String
    val customersOutstandingDebt: String
    val customersPayFullShortcut: String
    val customersNote: String
    val customersPhoneWhatsApp: String
    val customersName: String
    val customersPickFromContacts: String
    val customersSendWhatsAppReminder: String
    val customersEmptyTitle: String
    val customersNotFoundTitle: String
    val customersEmptyHint: String
    val customersNotFoundHint: String
    val customersClearSearch: String
    val customersAddFirst: String
    val customersOverdueCount: (Int) -> String
    val customersUnpaidCount: (Int) -> String
    val customersNoDueDate: String
    val customersDueDateOn: (String) -> String
    val customersOverdueSince: (String) -> String
    val customersNoDebtToPay: String
    val customersClosePaymentForm: String
    val customersConfirmPayment: String
    val customersContactUnreadable: String
    val customersContactPermissionDenied: String
    val customersPaymentRecorded: String
    val customersSaved: String
    val customersValidationSelectDebt: String
    val customersValidationAmountPositive: String
    val customersValidationAmountExceeds: String
    val customersValidationName: String

    // Debt Status
    val debtStatusPaid: String
    val debtStatusUnpaid: String
    val debtStatusPartiallyPaid: String
    val debtStatusOverdue: String

    // Transactions
    val transactionsTitle: String
    val transactionsSearchPlaceholder: String
    val transactionsDetailTitle: String
    val transactionsReprint: String
    val transactionsRefund: String
    val transactionsStatusCompleted: String
    val transactionsStatusDebt: String
    val transactionsStatusReturned: String
    val transactionsInvoiceNo: String
    val transactionsDate: String
    val transactionsCountWithPeriod: (Int, String) -> String
    val transactionsFilterPeriod: String
    val transactionsAddAction: String
    val transactionsLoading: String
    val transactionsLoadingDetail: String
    val transactionsEmptyTitle: String
    val transactionsEmptyHint: String
    val transactionsNoMatchTitle: String
    val transactionsNoMatchHint: String
    val transactionsShowAll: String
    val transactionsFilterAll: (Int) -> String
    val transactionsFilterOption: (String, Int) -> String
    val transactionsFieldCashier: String
    val transactionsFieldCustomer: String
    val transactionsFieldPaymentMethod: String
    val transactionsItemsTitle: String
    val transactionsFieldSubtotal: String
    val transactionsFieldDiscount: String
    val transactionsFieldTax: String
    val transactionsFieldRounding: String
    val transactionsFieldTotal: String
    val transactionsFieldPaid: String
    val transactionsFieldChange: String
    val transactionsFieldRemainingDebt: String
    val transactionsFieldDueDate: String
    val transactionsItemDiscountLine: (String) -> String
    val transactionsTableNumber: String
    val transactionsTableId: String
    val transactionsTableDateTime: String
    val transactionsTableMethod: String
    val transactionsTableStatus: String
    val transactionsTableTotal: String
    val transactionsActionMenu: (String) -> String
    val transactionsActionViewDetail: String
    val transactionsActionReprint: String
    val transactionsActionShare: String
    val transactionsActionRefund: String
    val transactionsMenuTitle: (String) -> String
    val transactionsNotFound: String
    val transactionsDetailLoadFailed: String
    val transactionsReceiptPrepareFailed: String

    // Returns
    val returnTitle: String
    val returnSubtitle: String
    val returnPurchasedQty: (String) -> String
    val returnQtyLabel: String
    val returnNoteLabel: String
    val returnSubmitButton: String
    val returnSubmittingButton: String
    val returnSuccessTitle: String
    val returnSuccessMsg: String
    val returnInvoiceLabel: (String) -> String
    val returnItemsTitle: String
    val returnLoading: String
    val returnValidationInvalidQuantity: String
    val returnValidationPositiveQuantity: String
    val returnValidationExceedsPurchased: String
    val returnValidationNothingSelected: String
    val returnLoadNotFound: String
    val returnLoadNoItems: String
    val returnSaveFailed: String
    val returnHardwareFailed: String
    val returnNetworkFailed: String
    val returnItemFallbackName: (Long) -> String

    // Reports
    val reportsTitle: String
    val reportsTotalTurnover: String
    val reportsNetProfit: String
    val reportsHppModal: String
    val reportsTxCount: String
    val reportsAverageTx: String
    val reportsSalesChart: String
    val reportsPaymentMethods: String
    val reportsTopProducts: String
    val reportsExportButton: String
    val reportsExportingMsg: String
    val reportsExportSuccess: (String) -> String
    val reportsExportFailed: String
    val reportsPresetToday: String
    val reportsPresetYesterday: String
    val reportsPresetLast7Days: String
    val reportsPresetLast30Days: String
    val reportsPresetThisMonth: String
    val reportsPresetThisYear: String
    val reportsPresetCustom: String
    val reportsLoading: String
    val reportsFilterPeriod: String
    val reportsExportAction: String
    val reportsRangeStart: String
    val reportsRangeEnd: String
    val reportsCompletedCount: (Int) -> String
    val reportsNetProfitHint: String
    val reportsHppHint: String
    val reportsAverageFromNotes: (Int) -> String
    val reportsNoteCount: (Int) -> String
    val reportsTopProductsHint: String
    val reportsSoldQuantity: (String) -> String
    val reportsPermissionNeeded: (String) -> String
    val reportsExportDialogTitle: String
    val reportsExportAdHint: (String) -> String
    val reportsExportFormat: String
    val reportsExportConfirm: String
    val reportsRangeCustomHint: String
    val reportsRangeDaysSuffix: (Int) -> String
    val reportsRangeTodayLower: String
    val reportsStatusFilterCompleted: String
    val reportsStatusFilterKasbon: String
    val reportsStatusFilterRefund: String
    val reportsExportErrorValidation: String
    val reportsExportErrorStorage: String
    val reportsExportErrorNotFound: String
    val reportsExportErrorHardware: String
    val reportsExportErrorNetwork: String
    val reportsExportErrorUnexpected: String

    // Settings
    val settingsTitle: String
    val settingsMenuLanguageDescription: String
    val settingsMenuPrinterDescription: String
    val settingsMenuQrisDescription: String
    val settingsMenuStoreDescription: String
    val settingsMenuStoreTax: (Int) -> String
    val settingsBackupTitle: String
    val settingsBackupMenuDescription: String
    val settingsBackupExportSection: String
    val settingsBackupExportHint: String
    val settingsBackupExportButton: String
    val settingsBackupExporting: String
    val settingsBackupExportedTitle: String
    val settingsBackupImportSection: String
    val settingsBackupImportHint: String
    val settingsBackupImportButton: String
    val settingsBackupImporting: String
    val settingsBackupImportedTitle: String
    val settingsBackupImportedRows: (Int) -> String
    val settingsBackupConfirmTitle: String
    val settingsBackupConfirmMessage: String
    val settingsBackupConfirmAction: String
    val settingsBackupErrorInvalid: String
    val settingsBackupErrorStorage: String
    val settingsBackupErrorUnexpected: String
    val settingsAboutTitle: String
    val settingsAboutMenuDescription: String
    val settingsAboutVersion: String
    val settingsPrivacyTitle: String
    val settingsPrivacyDescription: String
    val settingsPrivacyUnavailable: String
    val settingsStoreProfileTitle: String
    val settingsStoreNameLabel: String
    val settingsStoreNamePlaceholder: String
    val settingsCashierNameLabel: String
    val settingsCashierNamePlaceholder: String
    val settingsCashierNameHint: String
    val settingsTaxLabel: String
    val settingsRoundingLabel: String
    val settingsNoRounding: String
    val settingsQrisTitle: String
    val settingsQrisActiveMsg: String
    val settingsQrisNotSetMsg: String
    val settingsQrisPickImage: String
    val settingsQrisChangeImage: String
    val settingsQrisRemove: String
    val settingsLanguageTitle: String
    val settingsLanguageSubtitle: String
    val settingsLanguageSwitchedIndonesian: String
    val settingsLanguageSwitchedEnglish: String
    val settingsLanguageSwitchedChinese: String
    val settingsPrinterTitle: String
    val settingsPrinterStatusConnected: String
    val settingsPrinterStatusDisconnected: String
    val settingsPrinterStatusConnecting: String
    val settingsPrinterSelectDevice: String
    val settingsPrinterSimulator58: String
    val settingsPrinterSimulator80: String
    val settingsPrinterPaperWidth: String
    val settingsPrinterWidth58: String
    val settingsPrinterWidth80: String
    val settingsPrinterWidth58Detail: String
    val settingsPrinterWidth80Detail: String
    val settingsPrinterTestPrint: String
    val settingsPrinterPreviewTitle: String
    val settingsPrinterDisconnect: String
    val settingsPrinterPermissionNeeded: String
    val settingsPrinterTestSuccess: String
    val settingsPrinterNotFound: String
    val settingsPrinterNoneSelected: String
    val settingsPrinterSelectPrompt: String
    val settingsPrinterDeviceListTitle: String
    val settingsPrinterSimulatorLive: String
    val settingsPrinterConnect: String
    val settingsPrinterPreviewShort: String
    val settingsPrinterSimulatorNote: String
    val settingsPrinterPreviewLoading: String
    val settingsPrinterPreviewDescription: String
    val settingsPrinterSelectedMessage: (String) -> String
    val settingsPrinterConnectingMessage: (String) -> String
    val settingsPrinterConnectedMessage: (String) -> String
    val settingsPrinterDisconnectedMessage: String
    val settingsPrinterPickFirst: String
    val settingsPrinterTestPrinted: String
    val settingsPrinterWarning: String
    val settingsPaperWidthChanged: (Int) -> String
    val settingsQrisInstalled: String
    val settingsQrisNotConfigured: String
    val settingsQrisScanning: String
    val settingsQrisActiveDetail: String
    val settingsQrisEmptyDetail: String
    val settingsQrisUploadTitle: String
    val settingsQrisUploadHint: String
    val settingsQrisUploadTips: String
    val settingsQrisSavedPreview: String
    val settingsQrisSavedDescription: String
    val settingsQrisPayloadTitle: String
    val settingsQrisPayloadHint: String
    val settingsQrisSavedMessage: String
    val settingsQrisRemovedMessage: String
    val settingsQrisImageInvalid: String
    val settingsStoreNameRequired: String
    val settingsStoreSavedMessage: String
    val settingsSaveStore: String
    val settingsStoreSaved: String

    // More Screen Hub
    val moreHubTitle: String
    val moreHubSettings: String
    val moreHubCustomers: String
    val moreHubLanguage: String
    val moreHubPrinter: String
    val moreKasbonCount: (Int) -> String
    val moreCustomerCount: (Int) -> String
    val moreDefaultStoreName: String
    val moreDefaultCashierName: String
    val moreTaxSummary: (Int) -> String
    val moreShiftTitle: String
    val moreShiftActiveSubtitle: (String) -> String
    val moreShiftEmptySubtitle: String
    val moreShiftExpectedCash: String

    // Demo Data
    val demoDataTitle: String
    val demoDataLoad: String
    val demoDataClear: String
    val demoDataClearConfirmTitle: String
    val demoDataClearConfirmMsg: String
    val demoDataLoaded: (Int, Int, Int, Int) -> String
    val demoDataCleared: (Int, Int, Int) -> String
    val demoDataFailed: (String) -> String

    // Welcome
    val welcomeHeadline: String
    val welcomeStart: String
    val welcomeOfflineBadge: String
    val welcomeBodyPhone: String
    val welcomeBodyTablet: String
    val welcomeBulletTransactions: String
    val welcomeBulletStock: String
    val welcomeBulletProfit: String
    val welcomeOwnership: String
    val welcomeReadyOffline: String
    val welcomeFeatureCashier: String
    val welcomeFeatureInventory: String
    val welcomeFeatureProfit: String
    val welcomeShowcaseReceiptActive: String
    val welcomeShowcaseReceiptPaid: String
    val welcomeShowcaseReceiptOpen: String
    val welcomeShowcaseTotalDue: String
    val welcomeShowcaseChange: String
    val welcomeShowcaseStockTitle: String
    val welcomeShowcaseStockHint: String
    val welcomeShowcaseWholesaleActive: String
    val welcomeShowcaseProfitTitle: String
    val welcomeShowcaseProfitAmount: String
    val welcomeShowcaseTurnover: String
    val welcomeShowcaseProfitHint: String
    val welcomeShowcaseReady: String

    // Receipt
    val receiptDefaultStoreName: String
    val receiptInvoiceLabel: (String) -> String
    val receiptDateLabel: (String) -> String
    val receiptCashierLabel: (String) -> String
    val receiptCustomerLabel: (String) -> String
    val receiptMethodLabel: (String) -> String
    val receiptItemsTitle: String
    val receiptItemDiscount: (String) -> String
    val receiptSubtotal: (String) -> String
    val receiptDiscount: (String) -> String
    val receiptTax: (String) -> String
    val receiptRounding: (String) -> String
    val receiptTotal: (String) -> String
    val receiptTotalPlain: String
    val receiptPaid: (String) -> String
    val receiptChange: (String) -> String
    val receiptRemainingDebt: (String) -> String
    val receiptRemainingDebtShort: String
    val receiptDueDate: (String) -> String
    val receiptThankYou: String
    val receiptKeepAsProof: String
    val receiptInvoiceHeader: (String) -> String
    val receiptTestTitle: String
    val receiptPrintJobName: (String) -> String
    val receiptRenderFailed: String
    val receiptTestItemOne: String
    val receiptTestItemTwo: String
    val receiptTestPrinterWorking: String
    val receiptTestPaperWidth: (Int) -> String
    val receiptTestThankYou: String

    // Report export
    val exportFilePrefix: String
    val exportDone: String
    val exportDestination: (String) -> String
    val exportOpenFailed: String
    val exportShareFailed: String
    val exportTransactionSheetName: String
    val exportProductSheetName: String
    val exportColumnDate: String
    val exportColumnInvoice: String
    val exportColumnTotalSales: String
    val exportColumnPaymentMethod: String
    val exportColumnHpp: String
    val exportColumnProfit: String
    val exportColumnProductName: String
    val exportColumnQtySold: String
    val exportColumnSalesValue: String
    val cashierItemLabel: String
    val cashierItemCountLabel: String

    // Security
    val settingsSecurityTitle: String
    val settingsSecurityMenuDescription: String
    val securityMaskedValue: String
    val securityRestoreResetNotice: String
    val securityGateTitle: String
    val securityGateMessage: (String) -> String
    val securityGatePinLabel: String
    val securityGateUnlock: String
    val securityGateUseRecovery: String
    val securityGateRecoveryTitle: String
    val securityGateRecoveryMessage: String
    val securityGateRecoveryLabel: String
    val securityGateRecoveryConfirm: String
    val securityGateBackToPin: String
    val securityGateWrongPin: (Int) -> String
    val securityGateRetryIn: (Int) -> String
    val securityGateLockedFor: (Int) -> String
    val securityGateUnavailable: String
    val securityGateRecoveryInvalid: String
    val securityGateRecoveryUnavailable: String

    // Security screens
    val securityStatusTitle: String
    val securityStatusEnabled: String
    val securityStatusDisabled: String
    val securityStatusUpdatedAt: (String) -> String
    val securityStatusRecoveryReady: String
    val securityStatusRecoveryMissing: String
    val securitySessionOpen: String
    val securitySessionLock: String
    val securitySessionLocked: String
    val securityEnableAction: String
    val securityChangeAction: String
    val securityDisableAction: String
    val securityRegenerateAction: String
    val securityEnableIntro: String
    val securityEnableRecoveryNote: String
    val securityChangeIntro: String
    val securityDisableIntro: String
    val securityRegenerateIntro: String
    val securityCurrentPinLabel: String
    val securityNewPinLabel: String
    val securityConfirmPinLabel: String
    val securityPinLengthHint: (Int, Int) -> String
    val securityPinLengthError: (Int, Int) -> String
    val securityPinMismatch: String
    val securityPinRequired: String
    val securityWrongCurrentPin: String
    val securityPinNotConfigured: String
    val securityEnableSubmit: String
    val securityChangeSubmit: String
    val securityDisableSubmit: String
    val securityRegenerateSubmit: String
    val securityPinEnabledMessage: String
    val securityPinChangedMessage: String
    val securityPinDisabledMessage: String
    val securityRegeneratedMessage: String
    val securityRecoveryCodeTitle: String
    val securityRecoveryCodeOnceWarning: String
    val securityRecoveryCodePrint: String
    val securityRecoveryCodePrintJobName: String
    val securityRecoveryCodeAcknowledge: String
    val securityRecoveryCodeDone: String
    val securityRecoveryCodeUnsavedWarning: String
    val securityRecoveryCodeLostWarning: String
    val securityPrintCodeLabel: String
    val securityPrintInstructionOne: String
    val securityPrintInstructionTwo: String
    val securityPrintInstructionThree: String
    val securityPrintFooter: (String) -> String
    val securityAuditTitle: String
    val securityAuditEmpty: String
    val securityAuditLatest: (Int) -> String
    val securityAuditResultGranted: String
    val securityAuditResultDenied: String
    val securityAuditResultLocked: String
    val securityAuditResultRecoveryUsed: String
    val securityAuditResultPinEnabled: String
    val securityAuditResultPinChanged: String
    val securityAuditResultPinDisabled: String
    val securityAuditResultRecoveryRegenerated: String
    val securityAuditResultRestoreReset: String
    val securityAuditActionLabel: (CriticalAction) -> String
}

fun stringsFor(language: AppLanguage): PosStrings = when (language) {
    AppLanguage.INDONESIAN -> IdStrings
    AppLanguage.ENGLISH -> EnStrings
    AppLanguage.CHINESE -> ZhStrings
}

val LocalPosStrings = staticCompositionLocalOf<PosStrings> { IdStrings }
val LocalCurrentLanguage = staticCompositionLocalOf { AppLanguage.DEFAULT }
