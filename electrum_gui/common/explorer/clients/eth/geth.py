from functools import partial
from typing import List

from electrum_gui.common.basic.functional.require import require
from electrum_gui.common.basic.functional.text import force_text
from electrum_gui.common.basic.request.exceptions import JsonRPCException
from electrum_gui.common.basic.request.json_rpc import JsonRPCRequest
from electrum_gui.common.explorer.data.enums import TransactionStatus, TxBroadcastReceiptCode
from electrum_gui.common.explorer.data.exceptions import TransactionNotFound
from electrum_gui.common.explorer.data.interfaces import ExplorerInterface
from electrum_gui.common.explorer.data.objects import (
    Address,
    BlockHeader,
    ExplorerInfo,
    Transaction,
    TransactionFee,
    TxBroadcastReceipt,
)

_hex2int = partial(int, base=16)


class Geth(ExplorerInterface):
    __LAST_BLOCK__ = "latest"

    def __init__(self, url: str):
        self.rpc = JsonRPCRequest(url)

    def get_explorer_info(self) -> ExplorerInfo:
        block_number = self.rpc.call("eth_blockNumber", params=[])
        return ExplorerInfo(
            "geth",
            best_block_number=_hex2int(block_number),
            is_ready=True,
        )

    def get_address(self, address: str) -> Address:
        balance, nonce = self.rpc.batch_call(
            [
                ("eth_getBalance", [address, self.__LAST_BLOCK__]),
                ("eth_getTransactionCount", [address, self.__LAST_BLOCK__]),
            ]
        )  # Maybe __LAST_BLOCK__ refers to a different blocks in some case
        return Address(address=address, balance=_hex2int(balance), nonce=_hex2int(nonce))

    def get_transaction_by_txid(self, txid: str) -> Transaction:
        tx, receipt = self.rpc.batch_call(
            [
                ("eth_getTransactionByHash", [txid]),
                ("eth_getTransactionReceipt", [txid]),
            ]
        )
        if not tx:
            raise TransactionNotFound(txid)
        else:
            require(txid == tx.get("hash"))

        if receipt:
            block_header = BlockHeader(
                block_hash=receipt.get("blockHash", ""),
                block_number=_hex2int(receipt.get("blockNumber", "0x0")),
                block_time=0,
            )
            status = TransactionStatus.CONFIRMED if receipt.get("status") == "0x1" else TransactionStatus.REVERED
            gas_usage = _hex2int(receipt.get("gasUsed", "0x0"))
        else:
            block_header = None
            status = TransactionStatus.IN_MEMPOOL
            gas_usage = None

        gas_limit = _hex2int(tx.get("gas", "0x0"))
        fee = TransactionFee(
            limit=gas_limit,
            usage=gas_usage or gas_limit,
            price_per_unit=_hex2int(tx.get("gasPrice", "0x0")),
        )

        return Transaction(
            txid=txid,
            source=tx.get("from", ""),
            target=tx.get("to", ""),
            value=_hex2int(tx.get("value", "0x0")),
            status=status,
            block_header=block_header,
            fee=fee,
        )

    def search_txs_by_address(self, address: str) -> List[Transaction]:
        return []  # Cannot get TXs from Geth by address

    def broadcast_transaction(self, raw_tx: str) -> TxBroadcastReceipt:
        txid, is_success, receipt_code, receipt_message = None, False, TxBroadcastReceiptCode.UNKNOWN, ""

        try:
            txid = self.rpc.call("eth_sendRawTransaction", params=[raw_tx])
            is_success, receipt_code = True, TxBroadcastReceiptCode.SUCCESS
        except JsonRPCException as e:
            json_response = e.json_response
            receipt_code, receipt_message = TxBroadcastReceiptCode.UNEXPECTED_FAILED, force_text(json_response)

            if isinstance(json_response, dict) and "error" in json_response:
                error_message = json_response.get("error", dict()).get("message", "")
                receipt_message = error_message

                if "already known" in error_message:
                    receipt_code = TxBroadcastReceiptCode.ALREADY_KNOWN
                    is_success = True
                elif "nonce too low" in error_message:
                    receipt_code = TxBroadcastReceiptCode.NONCE_TOO_LOW

        return TxBroadcastReceipt(
            txid=txid,
            is_success=is_success,
            receipt_code=receipt_code,
            receipt_message=receipt_message,
        )
