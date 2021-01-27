//
//  OKMatchingInCirclesViewController.m
//  OneKey
//
//  Created by xiaoliang on 2020/12/10.
//  Copyright © 2020 OneKey. All rights reserved.
//

#import "OKMatchingInCirclesViewController.h"
#import "OKBluetoothViewCell.h"
#import "OKBluetoothViewCellModel.h"
#import "OKActivateDeviceSelectViewController.h"
#import "OKSetDeviceNameViewController.h"
#import "OKBlueManager.h"
#import "OKDeviceInfoModel.h"
#import "OKDiscoverNewDeviceViewController.h"
#import "OKSetDeviceNameViewController.h"
#import "OKSpecialEquipmentViewController.h"
#import "OKReceiveCoinViewController.h"
#import "OKSendCoinViewController.h"

@interface OKMatchingInCirclesViewController ()<OKBabyBluetoothManageDelegate,UITableViewDelegate,UITableViewDataSource>

@property (weak, nonatomic) IBOutlet UILabel *titleLabel;
@property (weak, nonatomic) IBOutlet UILabel *descLabel;
@property (weak, nonatomic) IBOutlet UIView *descLabelBgView;
@property (weak, nonatomic) IBOutlet UIView *midBgView;
@property (weak, nonatomic) IBOutlet UIView *bottomBgView;
@property (weak, nonatomic) IBOutlet UILabel *bLabel;
@property (weak, nonatomic) IBOutlet UIImageView *quanImage;
@property (nonatomic,strong)NSMutableArray *dataSource;
@property (nonatomic,strong)NSTimer *terminalTimer;
@property (nonatomic,assign)NSInteger count;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *completeCons;
@property (weak, nonatomic) IBOutlet UILabel *completetitleLabel;
@property (weak, nonatomic) IBOutlet UILabel *completetipsLabel;
@property (weak, nonatomic) IBOutlet UITableView *tableView;
@property (weak, nonatomic) IBOutlet UIView *completebgView;
@property (weak, nonatomic) IBOutlet UIButton *refreshBtn;
@end

@implementation OKMatchingInCirclesViewController

+ (instancetype)matchingInCirclesViewController
{
    return [[UIStoryboard storyboardWithName:@"Hardware" bundle:nil]instantiateViewControllerWithIdentifier:@"OKMatchingInCirclesViewController"];
}
- (NSMutableArray *)dataSource {
    if (!_dataSource) {
        _dataSource = [NSMutableArray new];
    }
    return _dataSource;
}
- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    [self stupUI];
    kOKBlueManager.delegate = self;
    self.terminalTimer = [NSTimer scheduledTimerWithTimeInterval:1 target:self selector:@selector(terminalTimerTickTock) userInfo:nil repeats:YES];
    [[NSRunLoop currentRunLoop] addTimer:self.terminalTimer forMode:NSRunLoopCommonModes];
    OKDeviceModel *model = [[OKDevicesManager sharedInstance]getDeviceModelWithID:kWalletManager.currentWalletInfo.device_id];
    if (![kOKBlueManager isConnectedName:model.deviceInfo.ble_name]) {
        [kOKBlueManager startScanPeripheral];
    }
    [self.completebgView setLayerRadius:20];
    [self.refreshBtn addTarget:self action:@selector(refreshBtnClick) forControlEvents:UIControlEventTouchUpInside];
    self.tableView.delegate = self;
    self.tableView.dataSource = self;
    self.tableView.tableFooterView = [UIView new];
}

- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
    [self setNavigationBarBackgroundColorWithClearColor];
}

- (void)terminalTimerTickTock
{
    OKWeakSelf(self)
    _count ++;
    if (_count == 3) {
        _count = 0;
        [weakself.terminalTimer invalidate];
        weakself.terminalTimer = nil;
        if (self.type == OKMatchingTypeTransfer || self.type == OKMatchingTypeReceiveCoin) {
            OKDeviceModel *model = [[OKDevicesManager sharedInstance]getDeviceModelWithID:kWalletManager.currentWalletInfo.device_id];
            if ([kOKBlueManager isConnectedName:model.deviceInfo.ble_name]) {
                [self subscribeComplete:@{}];
            }else{
                for (OKPeripheralInfo *infoModel in self.dataSource) {
                    if ([model.deviceInfo.ble_name isEqualToString:infoModel.peripheral.name]) {
                        [kOKBlueManager connectPeripheral:infoModel.peripheral];
                        return;
                    }
                }
            }
        }else{
            [weakself changeToListBgView];
            [weakself.tableView reloadData];
            weakself.completeCons.constant = - (SCREEN_HEIGHT - 170);
            [UIView animateWithDuration:0.5 animations:^{
                [weakself.view layoutIfNeeded];
            }];
        }
    }
}

- (void)changeToListBgView
{
    self.titleLabel.hidden = YES;
    self.midBgView.hidden = YES;
    self.bottomBgView.hidden = YES;
    self.descLabelBgView.hidden = YES;
    self.completetitleLabel.hidden = NO;
    self.completebgView.hidden = NO;
    self.completetipsLabel.hidden = NO;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return self.dataSource.count;
}
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *ID = @"OKBluetoothViewCell";
    OKBluetoothViewCell *cell = [tableView dequeueReusableCellWithIdentifier:ID];
    if (cell == nil) {
        cell = [[OKBluetoothViewCell alloc]initWithStyle:UITableViewCellStyleDefault reuseIdentifier:ID];
    }
    OKPeripheralInfo *info =  self.dataSource[indexPath.row];
    OKBluetoothViewCellModel *model = [OKBluetoothViewCellModel new];
    model.blueName = info.peripheral.name;
    cell.model = model;
    return cell;
}
- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    return 75;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    OKPeripheralInfo *peripheralInfo = self.dataSource[indexPath.row];
    [kOKBlueManager connectPeripheral:peripheralInfo.peripheral];
}
- (void)refreshBtnClick
{
    [kOKBlueManager startScanPeripheral];
}
- (void)stupUI
{
    self.titleLabel.text = MyLocalizedString(@"Open your hardware wallet and hold it close to your phone", nil);
    self.descLabel.text = MyLocalizedString(@"OneKey is currently supported (limited edition with coins and letters)", nil);
    self.title = MyLocalizedString(@"pairing", nil);
    [self.descLabelBgView setLayerRadius:35 * 0.5];
    [self.bottomBgView setLayerRadius:20];
    [self rotateImageView];
    self.completetitleLabel.text = MyLocalizedString(@"Open your hardware wallet and hold it close to your phone.", nil);
    self.completetitleLabel.hidden = YES;
}
- (void)rotateImageView {
    OKWeakSelf(self)
    CGFloat circleByOneSecond = 2.5f;
    [UIView animateWithDuration:1.f / circleByOneSecond
                          delay:0
                        options:UIViewAnimationOptionCurveLinear
                     animations:^{
        weakself.quanImage.transform = CGAffineTransformRotate(weakself.quanImage.transform, M_PI_2);
    }
                     completion:^(BOOL finished){
        [weakself rotateImageView];
    }];
}

- (void)dealloc
{
    [kOKBlueManager stopScanPeripheral];
}


#pragma mark OKBabyBluetoothManageDelegate
- (void)systemBluetoothClose {
    // 系统蓝牙被关闭、提示用户去开启蓝牙
    NSLog(@"系统蓝牙被关闭、提示用户去开启蓝牙");
}

- (void)sysytemBluetoothOpen {
    // 系统蓝牙已开启、开始扫描周边的蓝牙设备
    [kOKBlueManager startScanPeripheral];
}

- (void)getScanResultPeripherals:(NSArray *)peripheralInfoArr {
    NSLog(@"peripheralInfoArr == %@",peripheralInfoArr);
    
    OKWeakSelf(self)
    // 这里获取到扫描到的蓝牙外设数组、添加至数据源中
    if (self.dataSource.count>0) {
        [weakself.dataSource removeAllObjects];
    }
    [weakself.dataSource addObjectsFromArray:peripheralInfoArr];
}

- (void)connectSuccess {
    NSLog(@"connectSuccess");
}
- (void)readData:(NSData *)valueData {
    // 获取到蓝牙设备发来的数据
    NSLog(@"蓝牙发来的数据 = %@",valueData);
    NSLog(@"hexStringForData = %@",[NSData hexStringForData:valueData]);
}
- (void)connectFailed {
    // 连接失败、做连接失败的处理
}
- (void)subscribeComplete:(NSDictionary *)jsonDict
{
    switch (_type) {
        case OKMatchingTypeNone:
        {
            kOKBlueManager.currentReadDataStr = @"";
                if (jsonDict != nil) {
                    OKDeviceModel *deviceModel  = [[OKDeviceModel alloc]initWithJson:jsonDict];
                    kOKBlueManager.currentDeviceID = deviceModel.deviceInfo.device_id;
                    [[OKDevicesManager sharedInstance]addDevices:deviceModel];
                    dispatch_async(dispatch_get_main_queue(), ^{
                        if (deviceModel.deviceInfo.initialized ) {
                            if (deviceModel.deviceInfo.backup_only) {
                                OKSpecialEquipmentViewController *SpecialEquipmentVc = [OKSpecialEquipmentViewController specialEquipmentViewController];
                                [self.navigationController pushViewController:SpecialEquipmentVc animated:YES];
                            }else{
                                OKActivateDeviceSelectViewController *activateDeviceVc = [OKActivateDeviceSelectViewController activateDeviceSelectViewController];
                                [self.navigationController pushViewController:activateDeviceVc animated:YES];
                            }
                        }else{
                            OKDiscoverNewDeviceViewController *discoverNewDeviceVc = [OKDiscoverNewDeviceViewController discoverNewDeviceViewController];
                            [self.navigationController pushViewController:discoverNewDeviceVc animated:YES];
                        }
                    });
                }
        }
            break;
        case OKMatchingTypeBackup2Hw:
        {
            OKSetDeviceNameViewController *setDeviceNameVc = [OKSetDeviceNameViewController setDeviceNameViewController];
            setDeviceNameVc.type = OKMatchingTypeBackup2Hw;
            setDeviceNameVc.words = self.words;
            [self.navigationController pushViewController:setDeviceNameVc animated:YES];
        }
            break;
        case OKMatchingTypeTransfer:
        {
            dispatch_async(dispatch_get_main_queue(), ^{
                OKSendCoinViewController *sendCoinVc = [OKSendCoinViewController sendCoinViewController];
                [self.navigationController pushViewController:sendCoinVc animated:YES];
            });
        }
            break;
        case OKMatchingTypeReceiveCoin:
        {
            dispatch_async(dispatch_get_main_queue(), ^{
                OKReceiveCoinViewController *receiveCoinVc = [OKReceiveCoinViewController receiveCoinViewController];
                receiveCoinVc.coinType = kWalletManager.currentWalletInfo.coinType;
                receiveCoinVc.walletType = [kWalletManager getWalletDetailType];
                [self.navigationController pushViewController:receiveCoinVc animated:YES];
            });
        }
            break;
        default:
            break;
    }
}
@end
